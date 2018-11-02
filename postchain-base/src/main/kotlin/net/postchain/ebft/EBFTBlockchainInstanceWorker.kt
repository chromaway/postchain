package net.postchain.ebft

import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.BlockchainEngine
import net.postchain.core.NODE_ID_AUTO
import net.postchain.core.RestartHandler
import net.postchain.ebft.message.EbftMessage
import net.postchain.network.CommManager
import net.postchain.network.CommunicationManager
import kotlin.concurrent.thread

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
                communicationManager.peers().size,
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