// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.worker

import mu.KLogging
import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockchainEngine
import net.postchain.core.BlockchainProcess
import net.postchain.core.NodeStateTracker
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.BaseBlockManager
import net.postchain.ebft.BaseStatusManager
import net.postchain.ebft.StatusManager
import net.postchain.ebft.syncmanager.SyncManager
import net.postchain.ebft.syncmanager.validator.ValidatorSyncManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * A blockchain instance worker
 *
 * @param workerContext The stuff needed to start working.
 */
class ValidatorWorker(val workerContext: WorkerContext) : BlockchainProcess {

    companion object : KLogging()

    private lateinit var updateLoop: Thread
    private val shutdown = AtomicBoolean(false)

    private val blockDatabase: BaseBlockDatabase
    val syncManager: ValidatorSyncManager
    val networkAwareTxQueue: NetworkAwareTxQueue
    val nodeStateTracker = NodeStateTracker()
    val statusManager: StatusManager

    fun isInFastSyncMode(): Boolean {
        return syncManager.isInFastSync()
    }

    override fun getEngine(): BlockchainEngine {
        return workerContext.engine
    }

    init {
        val bestHeight = getEngine().getBlockQueries().getBestHeight().get()
        statusManager = BaseStatusManager(
                workerContext.signers.size,
                workerContext.nodeId,
                bestHeight + 1)

        blockDatabase = BaseBlockDatabase(
                getEngine(), getEngine().getBlockQueries(), workerContext.nodeId)

        val blockManager = BaseBlockManager(
                workerContext.processName,
                blockDatabase,
                statusManager,
                getEngine().getBlockBuildingStrategy())

        // Give the SyncManager the BaseTransactionQueue (part of workerContext) and not the network-aware one,
        // because we don't want tx forwarding/broadcasting when received through p2p network
        syncManager = ValidatorSyncManager(workerContext,
                statusManager,
                blockManager,
                blockDatabase,
                nodeStateTracker)

        networkAwareTxQueue = NetworkAwareTxQueue(
                getEngine().getTransactionQueue(),
                workerContext.communicationManager)

        statusManager.recomputeStatus()
        startUpdateLoop(syncManager)
    }

    /**
     * Create and run the [updateLoop] thread
     * @param syncManager the syncronization manager
     */
    protected fun startUpdateLoop(syncManager: SyncManager) {
        updateLoop = thread(name = "updateLoop-${workerContext.processName}") {
            while (!shutdown.get()) {
                try {
                    syncManager.update()
                    Thread.sleep(20)
                } catch (e: Exception) {
                    startUpdateErr("Failing to update", e)
                }
            }
        }
    }

    /**
     * Stop the postchain node
     */
    override fun shutdown() {
        shutdowDebug("Begin")
        syncManager.shutdown()
        shutdown.set(true)
        updateLoop.join()
        blockDatabase.stop()
        workerContext.shutdown()
        shutdowDebug("End")
    }

    // --------
    // Logging
    // --------

    private fun shutdowDebug(str: String) {
        if (logger.isDebugEnabled) {
            logger.debug("${workerContext.processName} shutdown() - $str")
        }
    }

    private fun startUpdateLog(str: String) {
        if (logger.isTraceEnabled) {
            logger.trace("${workerContext.processName} startUpdateLoop() -- $str")
        }
    }

    private fun startUpdateErr(str: String, e: Exception) {
        logger.error("${workerContext.processName} startUpdateLoop() -- $str", e)
    }
}