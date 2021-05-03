// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.worker

import mu.KLogging
import net.postchain.base.BaseBlockchainEngine
import net.postchain.base.BlockchainRid
import net.postchain.base.HistoricBlockchainContext
import net.postchain.base.data.BaseBlockStore
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.withReadConnection
import net.postchain.core.*
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.syncmanager.common.FastSyncParameters
import net.postchain.ebft.syncmanager.common.FastSynchronizer
import nl.komponents.kovenant.Promise
import java.lang.Thread.sleep
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
                          val historicBlockchainContext: HistoricBlockchainContext) : BlockchainProcess {

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
                        historicBlockchainContext.historicBrid
                )
                chainsToSyncFrom.addAll(historicBlockchainContext.aliases.keys)

                while (!shutdown.get()) {
                    val bestHeightSoFar = engine.getBlockQueries().getBestHeight().get()
                    logger.debug("Historic sync bc ${myBRID}, height: ${bestHeightSoFar}")

                    // try local sync first
                    for (brid in chainsToSyncFrom) {
                        if (shutdown.get()) break
                        if (brid == myBRID) continue
                        copyBlocksLocally(brid, blockDatabase)
                    }

                    // try syncing via network
                    for (brid in chainsToSyncFrom) {
                        if (shutdown.get()) break
                        copyBlocksNetwork(brid, myBRID, blockDatabase, params)
                        sleep(1000)
                    }
                    sleep(1000)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error syncing forkchain" }
            } finally {
                blockDatabase.stop()
                done.countDown()
            }
        }
    }

    /**
     * When network sync begins our starting point is the height where we left off
     * after copying blocks from our local DB.
     */
    private fun copyBlocksNetwork(
            brid: BlockchainRid, // the BC we are trying to pull blocks from
            myBRID: BlockchainRid, // our BC
            blockDatabase: BlockDatabase,
            params: FastSyncParameters) {

        if (brid == myBRID) {
            logger.debug("Historic sync: try network sync using own BRID")
            // using our own BRID
            workerContext.communicationManager.init()
            fastSynchronizer.syncUntilResponsiveNodesDrained()
            workerContext.communicationManager.shutdown()
        } else {
            val localChainID = getLocalChainId(brid)
            if (localChainID == null) {
                // we ONLY try syncing over network iff chain is not locally present
                // Reason for this is a bit complicated:
                //
                // TODO: When chain2 is started it migth fail because 0x02 is already
                // associated in conman to chain3.
                // So two ways to deal with this:
                // 1. eliminate race using mutexes and such
                // 2. create a new flag to make connection pre-emptible, so e.g.
                // chain3 can connect but if it's pre-emptible conman can disconnect it once chain2 can connect. we also need to make sure that fastsynchronizer understands disconnects.
                //
                //TL;DR: we can make it nicer at expense of higher complexity. "BRID is in DB thus we avoid this" is very simple rule.
                // Option 3: teach procman to distinguish restart from shutdown, e.g. keep a set of chainID which are "potentially will be launched soon".
                //Then we can ask procman if it's something to be concerned about.

                logger.debug("Historic sync: try network sync using historic BRID since chainId $localChainID is new" )
                try {
                    val historicWorkerContext = historicBlockchainContext.contextCreator(brid)
                    historicSynchronizer = FastSynchronizer(historicWorkerContext, blockDatabase, params)
                    historicSynchronizer!!.syncUntilResponsiveNodesDrained()
                    historicWorkerContext.communicationManager.shutdown()
                } catch (e: Exception) {
                    logger.error(e) { "Exception while attempting remote sync" }
                }
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

    private fun getLocalChainId(brid: BlockchainRid): Long? {
        return withReadConnection(storage, -1) {
            DatabaseAccess.of(it).getChainId(it, brid)
        }
    }

    private fun copyBlocksLocally(brid: BlockchainRid, newBlockDatabase: BlockDatabase) {
        val localChainID = getLocalChainId(brid)
        if (localChainID == null) return // can't sync locally
        logger.debug("Cross syncing locally from blockchain ${historicBlockchainContext.historicBrid}")

        val fromCtx = storage.openReadConnection(localChainID)
        val fromBstore = BaseBlockStore()
        var heightToCopy: Long = -2L
        var lastHeight: Long = -2L
        try {
            lastHeight = fromBstore.getLastBlockHeight(fromCtx)
            if (lastHeight == -1L) return // no block = nothing to do
            val ourHeight = getEngine().getBlockQueries().getBestHeight().get()
            if (lastHeight > ourHeight) {
                if (ourHeight > -1L) {
                    // Just a verification of Block RID being the same
                    verifyBlockAtHeightIsTheSame(fromBstore, fromCtx, ourHeight)
                }
                heightToCopy = ourHeight + 1
                var pendingPromise: Promise<Unit, java.lang.Exception>? = null
                while (!shutdown.get()) {
                    val historicBlock = getBlockFromStore(fromBstore, fromCtx, heightToCopy)
                    if (historicBlock == null) {
                        logger.debug("Done cross syncing height: $heightToCopy locally from blockchain ${historicBlockchainContext.historicBrid}")
                        break
                    }
                    pendingPromise = newBlockDatabase.addBlock(historicBlock)
                    if (pendingPromise != null) pendingPromise.get() // wait pending block
                    heightToCopy += 1
                }
                if (pendingPromise != null) pendingPromise.get() // wait pending block
            }

        } finally {
            logger.debug("Shutdown cross syncing, lastHeight: $lastHeight , got to height: $heightToCopy blocks locally from blockchain ${historicBlockchainContext.historicBrid}")
            storage.closeReadConnection(fromCtx)
        }
    }

    /**
     * We don't want to proceed if our last block isn't the same as the one in the historic chain.
     *
     * NOTE: Here we are actually comparing Block RIDs but using the [BlockchainRid] type for convenience.
     * TODO: Should we have a BlockRID type?
     */
    private fun verifyBlockAtHeightIsTheSame(fromBstore: BaseBlockStore, fromCtx: EContext, ourHeight: Long) {
        val historictBlockRID = BlockchainRid(fromBstore.getBlockRID(fromCtx, ourHeight)!!)
        val ourLastBlockRID = BlockchainRid(getEngine().getBlockQueries().getBlockRid(ourHeight).get()!!)
        if (historictBlockRID != ourLastBlockRID) {
            throw BadDataMistake(BadDataType.OTHER,
                    "Historic blockchain and fork chain disagree on block RID at height" +
                            "${ourHeight}. Historic: $historictBlockRID, fork: ${ourLastBlockRID}")
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