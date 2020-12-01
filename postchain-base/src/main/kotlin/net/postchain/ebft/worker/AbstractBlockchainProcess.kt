// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.worker

import mu.KLogging
import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainEngine
import net.postchain.core.BlockchainProcess
import net.postchain.core.NodeStateTracker
import net.postchain.debug.BlockchainProcessName
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.syncmanager.SyncManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * A blockchain instance model
 * @property blockDatabase wrapper class for the [BlockchainEngine] and [BlockQueries], starting new threads when running
 * @property syncManager
 * @property networkAwareTxQueue
 */
abstract class AbstractBlockchainProcess : BlockchainProcess {

    abstract val processName: BlockchainProcessName
    abstract val blockchainEngine: BlockchainEngine
    abstract val blockDatabase: BaseBlockDatabase
    abstract val syncManager: SyncManager
    abstract val nodeStateTracker: NodeStateTracker
    abstract val networkAwareTxQueue: NetworkAwareTxQueue

    private lateinit var updateLoop: Thread

    companion object : KLogging()

    override fun getEngine() = blockchainEngine

    private val shutdown = AtomicBoolean(false)

    /**
     * Create and run the [updateLoop] thread
     * @param syncManager the syncronization manager
     */
    protected fun startUpdateLoop(syncManager: SyncManager) {
        updateLoop = thread(name = "updateLoop-$processName") {
            while (!shutdown.get()) {
                try {
                    syncManager.update()
                    Thread.sleep(20)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Stop the postchain node
     */
    override fun shutdown() {
        shutdown.set(true)
        updateLoop.join()
        blockchainEngine.shutdown()
        blockDatabase.stop()
    }
}
