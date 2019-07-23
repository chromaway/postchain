package net.postchain.ebft.worker

import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.*
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.syncmanager.SyncManagerBase
import kotlin.concurrent.thread

/**
 * A blockchain instance model
 * @property blockDatabase wrapper class for the [BlockchainEngine] and [BlockQueries], starting new threads when running
 * @property syncManager
 * @property networkAwareTxQueue
 */
abstract class AbstractBlockchainProcess : BlockchainProcess {

    abstract val blockchainEngine: BlockchainEngine
    abstract val blockDatabase: BaseBlockDatabase
    abstract val syncManager: SyncManagerBase
    abstract val nodeStateTracker: NodeStateTracker
    abstract val networkAwareTxQueue: NetworkAwareTxQueue
    abstract val restartHandler: RestartHandler

    private lateinit var updateLoop: Thread

    override fun getEngine() = blockchainEngine

    /**
     * Create and run the [updateLoop] thread
     * @param syncManager the syncronization manager
     */
    protected fun startUpdateLoop(syncManager: SyncManagerBase) {
        updateLoop = thread(name = "updateLoop") {
            while (!Thread.interrupted()) {
                try {
                    syncManager.update()
                    if (blockchainEngine.isRestartNeeded) {
                        restartHandler()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    Thread.sleep(20)
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
        blockchainEngine.shutdown()
        blockDatabase.stop()
    }
}
