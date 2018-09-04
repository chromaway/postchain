package net.postchain.ebft

import net.postchain.api.rest.controller.PostchainModel
import net.postchain.api.rest.controller.RestApi
import net.postchain.base.*
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.ebft.message.EbftMessage
import net.postchain.network.PeerConnectionManager
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.atomic.AtomicBoolean
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
    val statusManager: BaseStatusManager
    val commManager: CommManager<EbftMessage>

    val txForwardingQueue: TransactionQueue
    val blockDatabase: BaseBlockDatabase
    val blockManager: BlockManager
    val syncManager: SyncManager

    val restApi: RestApi?
    val apiModel: PostchainModel?
}


class EBFTSynchronizationInfrastructure(val config: Configuration) : SynchronizationInfrastructure {
    val privKey: ByteArray
    val peerInfos: Array<PeerInfo>

    init {
        peerInfos = createPeerInfos(config)
        privKey = config.getString("messaging.privkey").hexStringToByteArray()
    }

    override fun makeBlockchainProcess(engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess {
        val configuration = engine.getConfiguration() as BaseBlockchainConfiguration // TODO
        val commConfiguration = BasePeerCommConfiguration(peerInfos,
                configuration.blockchainRID,
                configuration.configData.context.nodeID,
                SECP256K1CryptoSystem(),
                privKey
        )
        val connManager = makeConnManager(commConfiguration)
        return EBFTBlockchainInstanceWorker(engine,
                config,
                configuration.configData.context.nodeID,
                commConfiguration,
                connManager,
                restartHandler
        )
    }
}

/**
 * A blockchain instance worker
 *
 * @property updateLoop the main thread
 * @property stopMe boolean, which when set, will stop the thread [updateLoop]
 * @property peerInfos information relating to our peers
 */
class EBFTBlockchainInstanceWorker(
        private val engine: BlockchainEngine,
        config: Configuration,
        nodeIndex: Int,
        peerCommConfiguration: PeerCommConfiguration,
        connManager: PeerConnectionManager<EbftMessage>,
        val restartHandler: RestartHandler
) : BlockchainInstanceModel {

    lateinit var updateLoop: Thread
    val stopMe = AtomicBoolean(false)

    override val blockchainConfiguration: BlockchainConfiguration
    //private val storage: Storage
    override val statusManager: BaseStatusManager
    override val commManager: CommManager<EbftMessage>
    override val txForwardingQueue: TransactionQueue
    override val blockDatabase: BaseBlockDatabase
    override val blockManager: BlockManager
    override val syncManager: SyncManager
    override val restApi: RestApi?
    override val apiModel: PostchainModel?

    override fun getEngine(): BlockchainEngine {
        return engine
    }

    /**
     * Create and run the [updateLoop] thread until [stopMe] is set.
     *
     * @param syncManager the syncronization manager
     */
    protected fun startUpdateLoop(syncManager: SyncManager) {
        updateLoop = thread(name = "updateLoop") {
            while (true) {
                if (stopMe.get()) {
                    break
                }
                syncManager.update()
                Thread.sleep(50)
            }
        }
    }

    /**
     * Stop the postchain node
     */
    override fun shutdown() {
        // Ordering is important.
        // 1. Stop accepting API calls
        stopMe.set(true)
        restApi?.stop()
        // 2. Close the engine so that new blocks cant be started
        engine.shutdown()
        // 3. Close the listening port and all TCP connections
        // connManager.stop() // TODO
        // 4. Stop any in-progress blocks
        blockDatabase.stop()
    }

    init {
        blockchainConfiguration = engine.getConfiguration()
        val blockQueries = engine.getBlockQueries()
        val txQueue = engine.getTransactionQueue()

        val bestHeight = blockQueries.getBestHeight().get()
        statusManager = BaseStatusManager(peerCommConfiguration.peerInfo.size, nodeIndex, bestHeight + 1)
        commManager = makeCommManager(peerCommConfiguration, connManager)

        txForwardingQueue = NetworkAwareTxQueue(
                txQueue,
                commManager,
                nodeIndex)

        blockDatabase = BaseBlockDatabase(engine, blockQueries, nodeIndex)
        blockManager = BaseBlockManager(
                blockDatabase,
                statusManager,
                engine.getBlockBuildingStrategy())

        val port = config.getInt("api.port", 7740)
        if (port != -1) {
            val basePath = config.getString("api.basepath", "")
            restApi = RestApi(port, basePath)

            apiModel = PostchainModel(
                    txForwardingQueue,
                    blockchainConfiguration.getTransactionFactory(),
                    blockQueries as BaseBlockQueries)

            val blockchainRID = (blockchainConfiguration as BaseBlockchainConfiguration)
                    .blockchainRID.toHex()

            restApi.attachModel(blockchainRID, apiModel)

        } else {
            restApi = null
            apiModel = null
        }

        // Give the SyncManager the BaseTransactionQueue and not the network-aware one,
        // because we don't want tx forwarding/broadcasting when received through p2p network
        syncManager = SyncManager(statusManager, blockManager, blockDatabase, commManager, txQueue, blockchainConfiguration)
        statusManager.recomputeStatus()
        startUpdateLoop(syncManager)
    }

}