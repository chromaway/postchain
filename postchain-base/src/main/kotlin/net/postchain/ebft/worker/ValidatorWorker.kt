package net.postchain.ebft.worker

import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.BlockchainEngine
import net.postchain.core.NODE_ID_AUTO
import net.postchain.core.RestartHandler
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.BaseBlockManager
import net.postchain.ebft.BaseStatusManager
import net.postchain.ebft.BlockManager
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.syncmanager.SyncManagerBase
import net.postchain.ebft.syncmanager.ValidatorSyncManager
import net.postchain.network.CommunicationManager
import kotlin.concurrent.thread

/**
 * A blockchain instance worker
 *
 * @property updateLoop the main thread
 * @property blockManager manages intents and acts as a wrapper for [blockDatabase] and [statusManager]
 * @property statusManager manages the status of the consensus protocol
 */
class ValidatorWorker(
        private val engine: BlockchainEngine,
        nodeIndex: Int,
        communicationManager: CommunicationManager<EbftMessage>,
        val restartHandler: RestartHandler // TODO: Maybe redesign this feature
) : WorkerBase {

    private lateinit var updateLoop: Thread
    override val blockchainConfiguration: BlockchainConfiguration
    override val blockDatabase: BaseBlockDatabase
    val blockManager: BlockManager
    val statusManager: BaseStatusManager
    override val syncManager: ValidatorSyncManager
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
        syncManager = ValidatorSyncManager(
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
    private fun startUpdateLoop(syncManager: SyncManagerBase) {
        updateLoop = thread(name = "updateLoop") {
            while (!Thread.interrupted()) {
                syncManager.update()
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
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