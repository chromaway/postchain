package net.postchain.ebft.worker

import mu.KLogging
import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainEngine
import net.postchain.core.BlockchainProcess
import net.postchain.core.RestartHandler
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

    abstract val name: String
    abstract val blockchainEngine: BlockchainEngine
    abstract val blockDatabase: BaseBlockDatabase
    abstract val syncManager: SyncManagerBase
    abstract val networkAwareTxQueue: NetworkAwareTxQueue
    abstract val restartHandler: RestartHandler

    private lateinit var updateLoop: Thread
    companion object : KLogging()

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
                        logger.info("Node $name: Restarting of BlockchainProcess ${blockchainEngine.getConfiguration().chainID}")
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
