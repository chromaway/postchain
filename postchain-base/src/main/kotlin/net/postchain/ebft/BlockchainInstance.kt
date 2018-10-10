package net.postchain.ebft

import net.postchain.base.*
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.hexStringToByteArray
import net.postchain.core.*
import net.postchain.ebft.message.EbftMessage
import net.postchain.network.PeerConnectionManager
import org.apache.commons.configuration2.Configuration
import kotlin.concurrent.thread

/**
 * Retrieve peer information from config, including networking info and public keys
 *
 * @param config configuration
 * @return peer information
 */
fun createPeerInfos(config: Configuration): Array<PeerInfo> {
    // this is for testing only. We can prepare the configuration with a
    // special Array<PeerInfo> for dynamic ports
    val peerInfos = config.getProperty("testpeerinfos")
    if (peerInfos != null) {
        return if (peerInfos is PeerInfo) {
            arrayOf(peerInfos)
        } else {
            (peerInfos as List<PeerInfo>).toTypedArray()
        }
    }

    var peerCount = 0
    config.getKeys("node").forEach { peerCount++ }
    peerCount /= 4

    return Array(peerCount) {
        val port = config.getInt("node.$it.port")
        val host = config.getString("node.$it.host")
        val pubKey = config.getString("node.$it.pubkey").hexStringToByteArray()
        if (port == 0) {
            DynamicPortPeerInfo(host, pubKey)
        } else {
            PeerInfo(host, port, pubKey)
        }
    }
}


/**
 * A blockchain instance model
 *
 * @property blockchainConfiguration stateless object which describes an individual blockchain instance
 * @property storage handles back-end database connection and storage
 * @property blockQueries a collection of methods for various blockchain related queries
 * @property statusManager manages the status of the consensus protocol
 * @property commManager peer communication manager
 *
 * @property txQueue transaction queue for transactions received from peers. Will not be forwarded to other peers
 * @property txForwardingQueue transaction queue for transactions added locally via the REST API
 * @property blockStrategy strategy configurations for how to create new blocks
 * @property engine blockchain engine used for building and adding new blocks
 * @property blockDatabase wrapper class for the [engine] and [blockQueries], starting new threads when running
 * operations and handling exceptions
 * @property blockManager manages intents and acts as a wrapper for [blockDatabase] and [statusManager]
 * @property syncManager
 *
 * @property restApi contains information on the rest API, such as network parameters and available queries
 * @property apiModel
 */
interface BlockchainInstanceModel : BlockchainProcess {
    val blockchainConfiguration: BlockchainConfiguration
    val blockDatabase: BaseBlockDatabase
    val blockManager: BlockManager
    val statusManager: BaseStatusManager
    val syncManager: SyncManager
    val networkAwareTxQueue: NetworkAwareTxQueue
}

class EBFTSynchronizationInfrastructure(val config: Configuration) : SynchronizationInfrastructure {

    private val connManagers = mutableListOf<PeerConnectionManager<*>>()

    override fun shutdown() {
        connManagers.forEach { it.shutdown() }
    }

    override fun makeBlockchainProcess(engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess {
        val blockchainConfig = engine.getConfiguration() as BaseBlockchainConfiguration // TODO: [et]: Resolve type cast
        return EBFTBlockchainInstanceWorker(
                engine,
                blockchainConfig.configData.context.nodeID,
                buildCommunicationManager(blockchainConfig),
                restartHandler
        )
    }

    private fun buildCommunicationManager(blockchainConfig: BaseBlockchainConfiguration): CommManager<EbftMessage> {
        val communicationConfig = BasePeerCommConfiguration(
                createPeerInfos(config),
                blockchainConfig.blockchainRID,
                blockchainConfig.configData.context.nodeID,
                SECP256K1CryptoSystem(),
                privKey())

        val connectionManager = makeConnManager(communicationConfig)
        connManagers.add(connectionManager)
        return makeCommManager(communicationConfig, connectionManager)
    }

    private fun privKey(): ByteArray =
            config.getString("messaging.privkey").hexStringToByteArray()
}

/**
 * A blockchain instance worker
 *
 * @property updateLoop the main thread
 * @property peerInfos information relating to our peers
 */
open class EBFTBlockchainInstanceWorker(
        private val engine: BlockchainEngine,
        nodeIndex: Int,
        communicationManager: CommManager<EbftMessage>,
        val restartHandler: RestartHandler
) : BlockchainInstanceModel {

    private lateinit var updateLoop: Thread
    override val blockchainConfiguration: BlockchainConfiguration
    override val blockDatabase: BaseBlockDatabase
    override val blockManager: BlockManager
    override val statusManager: BaseStatusManager
    override val syncManager: SyncManager
    override val networkAwareTxQueue: NetworkAwareTxQueue

    init {
        blockchainConfiguration = engine.getConfiguration()

        val blockQueries = engine.getBlockQueries()
        val bestHeight = blockQueries.getBestHeight().get()
        statusManager = BaseStatusManager(
                communicationManager.peers.size,
                nodeIndex,
                bestHeight + 1)

        blockDatabase = BaseBlockDatabase(
                engine, blockQueries, nodeIndex)

        blockManager = BaseBlockManager(
                blockDatabase,
                statusManager,
                engine.getBlockBuildingStrategy())

        // Give the SyncManager the BaseTransactionQueue and not the network-aware one,
        // because we don't want tx forwarding/broadcasting when received through p2p network
        syncManager = SyncManager(
                statusManager,
                blockManager,
                blockDatabase,
                communicationManager,
                engine.getTransactionQueue(),
                blockchainConfiguration)

        networkAwareTxQueue = NetworkAwareTxQueue(
                engine.getTransactionQueue(),
                communicationManager,
                NODE_ID_AUTO)

        statusManager.recomputeStatus()
        startUpdateLoop(syncManager)
    }

    override fun getEngine(): BlockchainEngine {
        return engine
    }

    /**
     * Create and run the [updateLoop] thread
     *
     * @param syncManager the syncronization manager
     */
    private fun startUpdateLoop(syncManager: SyncManager) {
        updateLoop = thread(name = "updateLoop") {
            while (!Thread.interrupted()) {
                syncManager.update()
                Thread.sleep(50)
            }
        }
    }

    /**
     * Stop the postchain node
     */
    override fun shutdown() {
        updateLoop.interrupt()
        updateLoop.join()
        engine.shutdown()
        blockDatabase.stop()
    }
}