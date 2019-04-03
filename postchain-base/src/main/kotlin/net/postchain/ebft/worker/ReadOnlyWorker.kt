// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.ebft.worker

import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.BlockchainEngine
import net.postchain.core.NODE_ID_AUTO
import net.postchain.core.RestartHandler
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.syncmanager.ReplicaSyncManager
import net.postchain.network.CommunicationManager
import kotlin.concurrent.thread

/**
 * A blockchain instance replica worker
 *
 * @property updateLoop the main thread
 */
class ReadOnlyWorker(
        private val engine: BlockchainEngine,
        nodeIndex: Int,
        communicationManager: CommunicationManager<EbftMessage>,
        val restartHandler: RestartHandler
) : WorkerBase {

    private lateinit var updateLoop: Thread
    override val blockchainConfiguration: BlockchainConfiguration = engine.getConfiguration()
    override val blockDatabase: BaseBlockDatabase
    override val syncManager: ReplicaSyncManager
    override val networkAwareTxQueue: NetworkAwareTxQueue

    init {
        val blockQueries = engine.getBlockQueries()

        blockDatabase = BaseBlockDatabase(engine, blockQueries, nodeIndex)

        syncManager = ReplicaSyncManager()

        networkAwareTxQueue = NetworkAwareTxQueue(
                engine.getTransactionQueue(),
                communicationManager,
                NODE_ID_AUTO)

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
    private fun startUpdateLoop(syncManager: ReplicaSyncManager) {
        updateLoop = thread(name = "updateLoop") {
            while (!Thread.interrupted()) {
                try {
                    syncManager.update()
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
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