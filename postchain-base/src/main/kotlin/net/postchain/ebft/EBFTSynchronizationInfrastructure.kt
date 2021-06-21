// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft

import net.postchain.base.*
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.config.node.NodeConfig
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.debug.BlockchainProcessName
import net.postchain.debug.DiagnosticProperty
import net.postchain.debug.DiagnosticProperty.BLOCKCHAIN
import net.postchain.debug.DpNodeType
import net.postchain.debug.NodeDiagnosticContext
import net.postchain.ebft.message.Message
import net.postchain.ebft.worker.HistoricChainWorker
import net.postchain.ebft.worker.ReadOnlyWorker
import net.postchain.ebft.worker.ValidatorWorker
import net.postchain.ebft.worker.WorkerContext
import net.postchain.network.CommunicationManager
import net.postchain.network.netty2.NettyConnectorFactory
import net.postchain.network.x.DefaultXCommunicationManager
import net.postchain.network.x.DefaultXConnectionManager
import net.postchain.network.x.XConnectionManager
import net.postchain.network.x.XPeerID

@Suppress("JoinDeclarationAndAssignment")
class EBFTSynchronizationInfrastructure(
        val nodeConfigProvider: NodeConfigurationProvider,
        val nodeDiagnosticContext: NodeDiagnosticContext
) : SynchronizationInfrastructure {

    private val nodeConfig get() = nodeConfigProvider.getConfiguration()
    val connectionManager: XConnectionManager
    private val blockchainProcessesDiagnosticData = mutableMapOf<BlockchainRid, MutableMap<String, () -> Any>>()
    private val startWithFastSync: MutableMap<Long, Boolean> = mutableMapOf() // { chainId -> true/false }

    init {
        connectionManager = DefaultXConnectionManager(
                NettyConnectorFactory(),
                EbftPacketEncoderFactory(),
                EbftPacketDecoderFactory(),
                SECP256K1CryptoSystem())

        addBlockchainDiagnosticProperty()
    }

    override fun shutdown() {
        connectionManager.shutdown()
    }

    override fun makeBlockchainProcess(processName: BlockchainProcessName, engine: BlockchainEngine,
                                       historicBlockchainContext: HistoricBlockchainContext?): BlockchainProcess {
        val blockchainConfig = engine.getConfiguration() as BaseBlockchainConfiguration // TODO: [et]: Resolve type cast
        val unregisterBlockchainDiagnosticData: () -> Unit = {
            blockchainProcessesDiagnosticData.remove(blockchainConfig.blockchainRid)
        }
        val peerCommConfiguration = buildPeerCommConfiguration(nodeConfig, blockchainConfig)

        val workerContext = WorkerContext(
                processName, blockchainConfig.signers, engine,
                blockchainConfig.configData.context.nodeID,
                buildXCommunicationManager(processName, blockchainConfig, peerCommConfiguration),
                peerCommConfiguration,
                nodeConfig,
                unregisterBlockchainDiagnosticData,
                getStartWithFastSyncValue(blockchainConfig.chainID)
        )

        /*
        Block building is prohibited on FB if its current configuration has a historicBrid set.

        When starting a blockchain:

        If !hasHistoricBrid then do nothing special, proceed as we always did

        Otherwise:

        1 Sync from local-OB (if available) until drained
        2 Sync from remote-OB until drained or timeout
        3 Sync from FB until drained or timeout
        4 Goto 2
        */
        return if (historicBlockchainContext != null) {
            historicBlockchainContext.contextCreator = {
                val historicPeerCommConfiguration = if (it == historicBlockchainContext.historicBrid) {
                    buildPeerCommConfiguration(nodeConfig, blockchainConfig, historicBlockchainContext)
                } else {
                    // It's an ancestor brid for historicBrid
                    buildPeerCommConfigurationForAncestor(nodeConfig, historicBlockchainContext, it)
                }
                val histCommManager = buildXCommunicationManager(processName, blockchainConfig, historicPeerCommConfiguration, it)

                WorkerContext(processName, blockchainConfig.signers,
                        engine, blockchainConfig.configData.context.nodeID, histCommManager, historicPeerCommConfiguration,
                        nodeConfig, unregisterBlockchainDiagnosticData)

            }
            HistoricChainWorker(workerContext, historicBlockchainContext).also {
                registerBlockchainDiagnosticData(blockchainConfig.blockchainRid, DpNodeType.NODE_TYPE_SIGNER) {
                    "TODO: Implement getHeight()"
                }
            }
        } else if (blockchainConfig.configData.context.nodeID != NODE_ID_READ_ONLY) {
            ValidatorWorker(workerContext).also {
                registerBlockchainDiagnosticData(blockchainConfig.blockchainRid, DpNodeType.NODE_TYPE_SIGNER) {
                    it.syncManager.getHeight().toString()
                }
            }
        } else {
            ReadOnlyWorker(workerContext).also {
                registerBlockchainDiagnosticData(blockchainConfig.blockchainRid, DpNodeType.NODE_TYPE_REPLICA) {
                    it.getHeight().toString()
                }
            }
        }
    }

    override fun exitBlockchainProcess(process: BlockchainProcess) {
        val chainID = process.getEngine().getConfiguration().chainID
        startWithFastSync.remove(chainID) // remove status when process is gone
    }

    override fun restartBlockchainProcess(process: BlockchainProcess) {
        var fastSyncStatus = true
        val chainID = process.getEngine().getConfiguration().chainID
        if (process is ValidatorWorker) {
            fastSyncStatus = process.isInFastSyncMode()
        }
        startWithFastSync[chainID] = fastSyncStatus
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
            blockchainConfig: BaseBlockchainConfiguration,
            relevantPeerCommConfig: PeerCommConfiguration,
            blockchainRid: BlockchainRid? = null
    ): CommunicationManager<Message> {
        val effectiveRid = blockchainRid ?: blockchainConfig.blockchainRid
        val packetEncoder = EbftPacketEncoder(relevantPeerCommConfig, effectiveRid)
        val packetDecoder = EbftPacketDecoder(relevantPeerCommConfig)

        return DefaultXCommunicationManager(
                connectionManager,
                relevantPeerCommConfig,
                blockchainConfig.chainID,
                effectiveRid,
                packetEncoder,
                packetDecoder,
                processName
        ).apply { init() }
    }

    private fun buildPeerCommConfigurationForAncestor(nodeConfig: NodeConfig, historicBlockchainContext: HistoricBlockchainContext, ancBrid: BlockchainRid): PeerCommConfiguration {
        val myPeerID = XPeerID(nodeConfig.pubKeyByteArray)
        val peersThatServeAncestorBrid = historicBlockchainContext.ancestors[ancBrid]!!

        val relevantPeerMap = nodeConfig.peerInfoMap.filterKeys {
            it in peersThatServeAncestorBrid || it == myPeerID
        }

        return BasePeerCommConfiguration.build(
                relevantPeerMap.values,
                SECP256K1CryptoSystem(),
                nodeConfig.privKeyByteArray,
                nodeConfig.pubKeyByteArray)
    }

    /**
     * To calculate the [relevantPeerMap] we need to:
     *
     * 1. begin with the signers (from the BC config)
     * 2. add all NODE replicas (from node config)
     * 3. add BC replicas (from node config)
     *
     * TODO: Could getRelevantPeers() be a method inside [NodeConfig]?
     */
    private fun buildPeerCommConfiguration(nodeConfig: NodeConfig, blockchainConfig: BaseBlockchainConfiguration, historicBlockchainContext: HistoricBlockchainContext? = null): PeerCommConfiguration {
        val myPeerID = XPeerID(nodeConfig.pubKeyByteArray)
        val signers = blockchainConfig.signers.map { XPeerID(it) }
        val signersReplicas = signers.flatMap {
            nodeConfig.nodeReplicas[it] ?: listOf()
        }
        val blockchainReplicas = if (historicBlockchainContext != null) {
            (nodeConfig.blockchainReplicaNodes[historicBlockchainContext.historicBrid] ?: listOf()).union(
                    nodeConfig.blockchainReplicaNodes[blockchainConfig.blockchainRid] ?: listOf())
        } else {
            nodeConfig.blockchainReplicaNodes[blockchainConfig.blockchainRid] ?: listOf()
        }

        val relevantPeerMap = nodeConfig.peerInfoMap.filterKeys {
            it in signers || it in signersReplicas || it in blockchainReplicas || it == myPeerID
        }

        return BasePeerCommConfiguration.build(
                relevantPeerMap.values,
                SECP256K1CryptoSystem(),
                nodeConfig.privKeyByteArray,
                nodeConfig.pubKeyByteArray)
    }

    private fun addBlockchainDiagnosticProperty() {
        nodeDiagnosticContext.addProperty(BLOCKCHAIN) {
            val diagnosticData = blockchainProcessesDiagnosticData.toMutableMap()

            connectionManager.getPeersTopology().forEach { (blockchainRid, topology) ->
                diagnosticData.computeIfPresent(BlockchainRid.buildFromHex(blockchainRid)) { _, properties ->
                    properties.apply {
                        put(DiagnosticProperty.BLOCKCHAIN_NODE_PEERS.prettyName) { topology }
                    }
                }
            }

            diagnosticData
                    .mapValues { (_, v) ->
                        v.mapValues { (_, v2) -> v2() }
                    }
                    .values.toTypedArray()
        }
    }

    private fun registerBlockchainDiagnosticData(blockchainRid: BlockchainRid, nodeType: DpNodeType, getCurrentHeight: () -> String) {
        blockchainProcessesDiagnosticData[blockchainRid] = mutableMapOf<String, () -> Any>(
                DiagnosticProperty.BLOCKCHAIN_RID.prettyName to { blockchainRid.toHex() },
                DiagnosticProperty.BLOCKCHAIN_NODE_TYPE.prettyName to { nodeType.prettyName },
                DiagnosticProperty.BLOCKCHAIN_CURRENT_HEIGHT.prettyName to getCurrentHeight
        )
    }

    private fun getStartWithFastSyncValue(chainId: Long): Boolean {
        return startWithFastSync[chainId] ?: true
    }
}
