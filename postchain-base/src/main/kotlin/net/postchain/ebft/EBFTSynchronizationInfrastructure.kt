// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft

import net.postchain.base.BasePeerCommConfiguration
import net.postchain.base.BlockchainRid
import net.postchain.base.PeerCommConfiguration
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.hexStringToByteArray
import net.postchain.config.node.NodeConfig
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.debug.BlockchainProcessName
import net.postchain.debug.DiagnosticProperty
import net.postchain.debug.DiagnosticProperty.BLOCKCHAIN
import net.postchain.debug.DpNodeType
import net.postchain.debug.NodeDiagnosticContext
import net.postchain.ebft.message.Message
import net.postchain.ebft.worker.ReadOnlyWorker
import net.postchain.ebft.worker.ValidatorWorker
import net.postchain.network.CommunicationManager
import net.postchain.network.netty2.NettyConnectorFactory
import net.postchain.network.x.DefaultXCommunicationManager
import net.postchain.network.x.DefaultXConnectionManager
import net.postchain.network.x.XConnectionManager

class EBFTSynchronizationInfrastructure(
        val nodeConfigProvider: NodeConfigurationProvider,
        val nodeDiagnosticContext: NodeDiagnosticContext
) : SynchronizationInfrastructure {

    private val nodeConfig get() = nodeConfigProvider.getConfiguration()
    val peerCommConfiguration: PeerCommConfiguration
    val connectionManager: XConnectionManager
    private val blockchainProcessesDiagnosticData = mutableMapOf<BlockchainRid, MutableMap<String, Any>>()

    init {
        peerCommConfiguration = buildPeerCommConfiguration(nodeConfig)
        connectionManager = DefaultXConnectionManager(
                NettyConnectorFactory(),
                peerCommConfiguration,
                EbftPacketEncoderFactory(),
                EbftPacketDecoderFactory(),
                SECP256K1CryptoSystem())

        addBlockchainDiagnosticProperty()
    }

    override fun shutdown() {
        connectionManager.shutdown()
    }

    override fun makeBlockchainProcess(processName: BlockchainProcessName, engine: BlockchainEngine): BlockchainProcess {
        val blockchainConfig = engine.getConfiguration() as BaseBlockchainConfiguration // TODO: [et]: Resolve type cast
        val unregisterBlockchainDiagnosticData: () -> Unit = {
            blockchainProcessesDiagnosticData.remove(blockchainConfig.blockchainRid)
        }

        return if (blockchainConfig.configData.context.nodeID != NODE_ID_READ_ONLY) {
            registerBlockchainDiagnosticData(blockchainConfig.blockchainRid, DpNodeType.NODE_TYPE_VALIDATOR)

            ValidatorWorker(
                    processName,
                    blockchainConfig.signers,
                    engine,
                    blockchainConfig.configData.context.nodeID,
                    buildXCommunicationManager(processName, blockchainConfig),
                    unregisterBlockchainDiagnosticData
            )
        } else {
            registerBlockchainDiagnosticData(blockchainConfig.blockchainRid, DpNodeType.NODE_TYPE_REPLICA)

            ReadOnlyWorker(
                    processName,
                    blockchainConfig.signers,
                    engine,
                    buildXCommunicationManager(processName, blockchainConfig),
                    unregisterBlockchainDiagnosticData)
        }
    }

    @Deprecated("POS-90")
    private fun validateConfigurations(nodeConfig: NodeConfig, blockchainConfig: BaseBlockchainConfiguration) {
        val chainPeers = blockchainConfig.signers.map { it.byteArrayKeyOf() }

        val unreachableSigners = chainPeers.filter { !nodeConfig.peerInfoMap.contains(it) }
        require(unreachableSigners.isEmpty()) {
            "Invalid blockchain config: unreachable signers have been detected: " +
                    chainPeers.toTypedArray().contentToString()
        }
    }

    private fun buildXCommunicationManager(
            processName: BlockchainProcessName,
            blockchainConfig: BaseBlockchainConfiguration
    ): CommunicationManager<Message> {
        val packetEncoder = EbftPacketEncoder(peerCommConfiguration, blockchainConfig.blockchainRid)
        val packetDecoder = EbftPacketDecoder(peerCommConfiguration)

        return DefaultXCommunicationManager(
                connectionManager,
                peerCommConfiguration,
                blockchainConfig.chainID,
                blockchainConfig.blockchainRid,
                packetEncoder,
                packetDecoder,
                processName
        ).apply { init() }
    }

    private fun buildPeerCommConfiguration(nodeConfig: NodeConfig): PeerCommConfiguration {
        return BasePeerCommConfiguration.build(
                nodeConfig.peerInfoMap,
                SECP256K1CryptoSystem(),
                nodeConfig.privKeyByteArray,
                nodeConfig.pubKey.hexStringToByteArray())
    }

    private fun addBlockchainDiagnosticProperty() {
        nodeDiagnosticContext.addProperty(BLOCKCHAIN) {
            val diagnosticData = blockchainProcessesDiagnosticData.toMutableMap()

            connectionManager.getPeersTopology().forEach { (blockchainRid, topology) ->
                diagnosticData.computeIfPresent(BlockchainRid.buildFromHex(blockchainRid)) { _, properties ->
                    properties.apply {
                        put(DiagnosticProperty.BLOCKCHAIN_NODE_PEERS.prettyName, topology)
                    }
                }
            }

            diagnosticData.values.toTypedArray()
        }
    }

    private fun registerBlockchainDiagnosticData(blockchainRid: BlockchainRid, nodeType: DpNodeType) {
        blockchainProcessesDiagnosticData[blockchainRid] = mutableMapOf<String, Any>(
                DiagnosticProperty.BLOCKCHAIN_RID.prettyName to blockchainRid.toHex(),
                DiagnosticProperty.BLOCKCHAIN_NODE_TYPE.prettyName to nodeType.prettyName
        )
    }
}