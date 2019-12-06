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
import net.postchain.debug.DiagnosticProperty
import net.postchain.debug.DiagnosticProperty.BLOCKCHAIN
import net.postchain.debug.DiagnosticProperty.PEERS_TOPOLOGY
import net.postchain.debug.NodeDiagnosticContext
import net.postchain.ebft.message.Message
import net.postchain.ebft.worker.ReadOnlyWorker
import net.postchain.ebft.worker.ValidatorWorker
import net.postchain.network.CommunicationManager
import net.postchain.network.netty2.NettyConnectorFactory
import net.postchain.network.x.DefaultXCommunicationManager
import net.postchain.network.x.DefaultXConnectionManager
import net.postchain.network.x.XConnectionManager
import kotlin.reflect.KClass
import net.postchain.network.x.XPeerID

class EBFTSynchronizationInfrastructure(
        val nodeConfigProvider: NodeConfigurationProvider,
        val nodeDiagnosticContext: NodeDiagnosticContext
) : SynchronizationInfrastructure {

    private val nodeConfig get() = nodeConfigProvider.getConfiguration()
    val connectionManager: XConnectionManager
    private val blockchainProcessesDiagnosticData = mutableMapOf<BlockchainRid, Map<String, String>>()

    init {
        connectionManager = DefaultXConnectionManager(
                NettyConnectorFactory(),
                buildInternalPeerCommConfiguration(nodeConfig),
                EbftPacketEncoderFactory(),
                EbftPacketDecoderFactory(),
                SECP256K1CryptoSystem())

        nodeDiagnosticContext.addProperty(PEERS_TOPOLOGY) { connectionManager.getPeersTopology() }
        nodeDiagnosticContext.addProperty(BLOCKCHAIN) { blockchainProcessesDiagnosticData.values.toTypedArray() }
    }

    override fun shutdown() {
        connectionManager.shutdown()
    }

    override fun makeBlockchainProcess(processName: String, engine: BlockchainEngine): BlockchainProcess {
        val blockchainConfig = engine.getConfiguration() as BaseBlockchainConfiguration // TODO: [et]: Resolve type cast

        return if (blockchainConfig.configData.context.nodeID != NODE_ID_READ_ONLY) {
            registerBlockchainDiagnosticData(blockchainConfig.blockchainRID, ValidatorWorker::class)

            ValidatorWorker(
                    processName,
                    blockchainConfig.signers,
                    engine,
                    blockchainConfig.configData.context.nodeID,
                    buildXCommunicationManager(processName, blockchainConfig, false))
        } else {
            registerBlockchainDiagnosticData(blockchainConfig.blockchainRID, ReadOnlyWorker::class)

            ReadOnlyWorker(
                    processName,
                    blockchainConfig.signers,
                    engine,
                    buildXCommunicationManager(processName, blockchainConfig, true))
        }
    }

    private fun registerBlockchainDiagnosticData(blockchainRid: BlockchainRid, nodeType: KClass<out BlockchainProcess>) {
        blockchainProcessesDiagnosticData[blockchainRid] = mapOf(
                DiagnosticProperty.BLOCKCHAIN_RID.prettyName to blockchainRid.toHex(),
                DiagnosticProperty.BLOCKCHAIN_NODE_TYPE.prettyName to (nodeType.simpleName ?: nodeType.toString())
        )
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
            processName: String,
            blockchainConfig: BaseBlockchainConfiguration,
            isReplica: Boolean
    ): CommunicationManager<Message> {
        val nodeConfigCopy = nodeConfig

        val signers = blockchainConfig.signers.map { XPeerID(it) }
        val signerReplicas = signers.flatMap {
            nodeConfigCopy.nodeReplicas[it] ?: listOf()
        }
        val blockchainReplicaNodes = nodeConfigCopy.blockchainReplicaNodes[
                blockchainConfig.blockchainRID
        ] ?: listOf<XPeerID>();
        val relevantPeerMap = nodeConfigCopy.peerInfoMap.filterKeys {
            signers.contains(it)
                    || signerReplicas.contains(it)
                    || blockchainReplicaNodes.contains(it)
        }

        val communicationConfig = BasePeerCommConfiguration.build(
                relevantPeerMap,
                SECP256K1CryptoSystem(),
                nodeConfigCopy.privKeyByteArray,
                nodeConfigCopy.pubKey.hexStringToByteArray())

        val packetEncoder = EbftPacketEncoder(communicationConfig, blockchainConfig.blockchainRID)
        val packetDecoder = EbftPacketDecoder(communicationConfig)

        return DefaultXCommunicationManager(
                connectionManager,
                communicationConfig,
                blockchainConfig.chainID,
                blockchainConfig.blockchainRID,
                packetEncoder,
                packetDecoder,
                processName
        ).apply { init() }
    }

    private fun buildInternalPeerCommConfiguration(nodeConfig: NodeConfig): PeerCommConfiguration {
        return BasePeerCommConfiguration.build(
                nodeConfig.peerInfoMap,
                SECP256K1CryptoSystem(),
                nodeConfig.privKeyByteArray,
                nodeConfig.pubKey.hexStringToByteArray())
    }
}