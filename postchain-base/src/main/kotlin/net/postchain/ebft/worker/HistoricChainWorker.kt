// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.worker

import mu.KLogging
import net.postchain.base.HistoricBlockchain
import net.postchain.common.toHex
import net.postchain.core.BlockchainProcess
import net.postchain.core.NODE_ID_READ_ONLY
import net.postchain.core.ProgrammerMistake
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.syncmanager.common.FastSyncParameters
import net.postchain.ebft.syncmanager.common.FastSynchronizer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Worker that synchronizes a blockchain using blocks from another blockchain, historicBrid.
 * The idea with this worker is to be able to fork a blockchain reliably.
 * OB = original blockchain (historic Brid)
 * FB = forked blockchain
 *
 * 1 Sync from local-OB (if available) until drained
 * 2 Sync from remote-FB until drained or timeout
 * 3 Sync from remote-OB until drained or timeout
 * 4 Goto 1
 *
 */
class HistoricChainWorker(val workerContext: WorkerContext,
                          val historicBlockchain: HistoricBlockchain) : BlockchainProcess {

    override fun getEngine() = workerContext.engine

    private val fastSynchronizer: FastSynchronizer
    private var historicSynchronizer: FastSynchronizer? = null
    private val done = CountDownLatch(1)
    private val shutdown = AtomicBoolean(false)

    companion object: KLogging()
    init {
        val blockDatabase = BaseBlockDatabase(
                getEngine(), getEngine().getBlockQueries(), NODE_ID_READ_ONLY)
        val params = FastSyncParameters()
        params.exitDelay = workerContext.nodeConfig.fastSyncExitDelay
        params.jobTimeout = workerContext.nodeConfig.fastSyncJobTimeout
        fastSynchronizer = FastSynchronizer(workerContext, blockDatabase, params)

        thread(name = "historicSync-${workerContext.processName}") {
            try {
                var firstLoop = true
                while (!shutdown.get()) {
                    copyBlocksLocally(blockDatabase)

                    // CommManager is already inited when this worker is started. Hack around that.
                    // We need to shutdown/init the communication managers for both fastsynchronizers
                    // in this while loop so that they don't step on eachother's toes.
                    if (!firstLoop) {
                        workerContext.communicationManager.init()
                    } else {
                        firstLoop = false
                    }

                    // Even if we have historic blockchain locally, we might not be actively running
                    // that blockchain, which means that we have to sync from FB or OB nodes
                    // until we catch up.
                    // A potential optimization for the future:
                    // We can check whether there's a process for historic blockchain. If there
                    // is, we can keep syncing locally, because the OB process will
                    // sync from the network. But let's keep it simple for now

                    fastSynchronizer.syncUntilResponsiveNodesDrained()
                    if (shutdown.get()) {
                        break
                    }
                    workerContext.communicationManager.shutdown()
                    val historicWorkerContext = historicBlockchain.contextCreator()
                    historicSynchronizer = FastSynchronizer(historicWorkerContext, blockDatabase, params)
                    historicSynchronizer!!.syncUntilResponsiveNodesDrained()
                    historicWorkerContext.communicationManager.shutdown()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error syncing forkchain" }
            } finally {
                blockDatabase.stop()
                done.countDown()
            }
        }
    }

    private fun copyBlocksLocally(newBlockDatabase: BlockDatabase) {
        val historicQueries = historicBlockchain.historicBlockQueries
        if (historicQueries == null) {
            logger.debug("Can't cross sync locally, doesn't have blockchain ${historicBlockchain.historicBrid}")
            return
        }
        logger.debug("Cross syncing locally from blockchain ${historicBlockchain.historicBrid}")
        val newBlockQueries = workerContext.engine.getBlockQueries()
        var newBestHeight = newBlockQueries.getBestHeight().get()
        if (newBestHeight > -1) {
            // Verify that our best block matches the corresponding block in
            // the old blockchain
            val newBestBlockHeader = newBlockQueries.getBlockAtHeight(newBestHeight, false).get()!!
            val historicBlockRid = historicQueries.getBlockRid(newBestHeight).get() ?: return // We have no more blocks locally
            if (!historicBlockRid.contentEquals(newBestBlockHeader.header.blockRID)) {
                throw ProgrammerMistake("Historic blockchain and fork chain disagree on block RID at height " +
                        "$newBestHeight. Historic: $historicBlockRid, fork: ${newBestBlockHeader.header.blockRID}")
            }
        }
        val startHeight = newBestHeight
        // We have checked that the fork so far matches the blocks in the historic blockchain.
        // Now let's start the copying process and exit on shutdown or all blocks copied
        while (!shutdown.get()) {
            val historicBlock = historicQueries.getBlockAtHeight(++newBestHeight).get()
            if (historicBlock == null) {
                logger.debug("Done cross syncing ${newBestHeight-startHeight-1} blocks locally from blockchain ${historicBlockchain.historicBrid}")
                return
            }
            logger.trace("Cross syncing block ${historicBlock.header.blockRID.toHex()} at height $newBestHeight from blockchain ${historicBlockchain.historicBrid}")
            newBlockDatabase.addBlock(historicBlock).get()
        }
        logger.debug("Shutdown cross syncing ${newBestHeight-startHeight} blocks locally from blockchain ${historicBlockchain.historicBrid}")
    }

    override fun shutdown() {
        shutdown.set(true)
        historicSynchronizer?.shutdown()
        fastSynchronizer.shutdown()
        done.await()
        workerContext.shutdown()
    }
}