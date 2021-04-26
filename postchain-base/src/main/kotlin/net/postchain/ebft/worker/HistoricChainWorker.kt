// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.worker

import mu.KLogging
import net.postchain.base.BaseBlockchainEngine
import net.postchain.base.BlockchainRid
import net.postchain.base.HistoricBlockchain
import net.postchain.base.data.BaseBlockStore
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.withReadConnection
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.syncmanager.common.FastSyncParameters
import net.postchain.ebft.syncmanager.common.FastSynchronizer
import nl.komponents.kovenant.Promise
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
    private val storage = (workerContext.engine as BaseBlockchainEngine).storage
    private val bs = BaseBlockStore()

    companion object : KLogging()

    init {
        val engine = getEngine()
        val myBRID = engine.getConfiguration().blockchainRid
        val blockDatabase = BaseBlockDatabase(
                engine, engine.getBlockQueries(), NODE_ID_READ_ONLY)
        val params = FastSyncParameters()
        params.exitDelay = workerContext.nodeConfig.fastSyncExitDelay
        params.jobTimeout = workerContext.nodeConfig.fastSyncJobTimeout
        fastSynchronizer = FastSynchronizer(workerContext, blockDatabase, params)

        thread(name = "historicSync-${workerContext.processName}") {
            try {
                val chainsToSyncFrom = mutableListOf(
                        myBRID,
                        historicBlockchain.historicBrid
                )
                chainsToSyncFrom.addAll(historicBlockchain.aliases.keys)

                while (!shutdown.get()) {
                    // try local sync first
                    for (brid in chainsToSyncFrom) {
                        if (shutdown.get()) break
                        copyBlocksLocally(brid, blockDatabase)
                    }

                    // try syncing via network
                    for (brid in chainsToSyncFrom) {
                        if (shutdown.get()) break
                        if (brid == myBRID) {
                            // using our own BRID
                            workerContext.communicationManager.init()
                            fastSynchronizer.syncUntilResponsiveNodesDrained()
                            workerContext.communicationManager.shutdown()
                        } else {
                            val localChainID = withReadConnection(storage, -1) {
                                bs.getChainId(it, brid)
                            }
                            if (localChainID == null) {
                                // we try syncing over network iff chain is not locally present
                                try {
                                    val historicWorkerContext = historicBlockchain.contextCreator(brid)
                                    historicSynchronizer = FastSynchronizer(historicWorkerContext, blockDatabase, params)
                                    historicSynchronizer!!.syncUntilResponsiveNodesDrained()
                                    historicWorkerContext.communicationManager.shutdown()
                                } catch (e: Exception) {
                                    logger.error(e) { "Exception while attempting remote sync" }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error syncing forkchain" }
            } finally {
                blockDatabase.stop()
                done.countDown()
            }
        }
    }

    private fun getBlockFromStore(blockStore: BaseBlockStore, ctx: EContext, height: Long): BlockDataWithWitness? {
        val blockchainConfiguration = getEngine().getConfiguration()
        val blockRID = blockStore.getBlockRID(ctx, height)
        if (blockRID == null) {
            return null
        } else {
            val headerBytes = blockStore.getBlockHeader(ctx, blockRID)
            val witnessBytes = blockStore.getWitnessData(ctx, blockRID)
            val txBytes = blockStore.getBlockTransactions(ctx, blockRID)
            // note: We are decoding header of other blockchain using our configuration.
            // We believe we have a right to do that because it should be sufficiently compatible
            val header = blockchainConfiguration.decodeBlockHeader(headerBytes)
            val witness = blockchainConfiguration.decodeWitness(witnessBytes)

            return BlockDataWithWitness(header, txBytes, witness)
        }
    }

    private fun copyBlocksLocally(brid: BlockchainRid, newBlockDatabase: BlockDatabase) {
        val localChainID = withReadConnection(storage, -1) {
            DatabaseAccess.of(it).getChainId(it, brid)
        }
        if (localChainID == null) return // can't sync locally
        logger.debug("Cross syncing locally from blockchain ${historicBlockchain.historicBrid}")


        val fromCtx = storage.openReadConnection(localChainID)
        val fromBstore = BaseBlockStore()
        try {
            val lastHeight = fromBstore.getLastBlockHeight(fromCtx)
            if (lastHeight == -1L) return // no block = nothing to do
            val ourHeight = getEngine().getBlockQueries().getBestHeight().get()
            if (lastHeight > ourHeight) {
                if (ourHeight > -1L) {
                    val historictBRID = BlockchainRid(fromBstore.getBlockRID(fromCtx, ourHeight)!!)
                    val ourLastBRID = BlockchainRid(getEngine().getBlockQueries().getBlockRid(ourHeight).get()!!)
                    if (historictBRID != ourLastBRID) {
                        throw BadDataMistake(BadDataType.OTHER,
                                "Historic blockchain and fork chain disagree on block RID at height" +
                                        "${ourHeight}. Historic: $historictBRID, fork: ${ourLastBRID}")
                    }
                }
                var heightToCopy = ourHeight + 1
                var pendingPromise: Promise<Unit, java.lang.Exception>? = null
                while (!shutdown.get()) {
                    val historicBlock = getBlockFromStore(fromBstore, fromCtx, heightToCopy)
                    if (historicBlock == null) {
                        // TODO: better message
                        logger.debug("Done cross syncing ... blocks locally from blockchain ${historicBlockchain.historicBrid}")
                        break
                    }
                    pendingPromise = newBlockDatabase.addBlock(historicBlock)
                    heightToCopy += 1
                }
                if (pendingPromise != null) pendingPromise.get() // wait pending block
            }

        } finally {
            storage.closeReadConnection(fromCtx)
        }
    }

    override fun shutdown() {
        shutdown.set(true)
        historicSynchronizer?.shutdown()
        fastSynchronizer.shutdown()
        done.await()
        workerContext.shutdown()
    }
}