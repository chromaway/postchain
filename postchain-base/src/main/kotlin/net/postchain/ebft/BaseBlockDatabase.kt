// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft

import mu.KLogging
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.debug.BlockTrace
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A wrapper class for the [engine] and [blockQueries], starting new threads when running
 *
 * NOTE: Re logging
 * Looks like this class used to do too much logging, so now everything has been scaled down one notch
 * (debug -> trace, etc). IMO this is better than blocking the logging from YAML (which might be hard to remember)
 */
class BaseBlockDatabase(
        private val engine: BlockchainEngine,
        private val blockQueries: BlockQueries,
        val nodeIndex: Int
) : BlockDatabase {

    private val executor = Executors.newSingleThreadExecutor {
        Thread(it, "$nodeIndex-BaseBlockDatabaseWorker")
                .apply {
                    isDaemon = true // So it can't block the JVM from exiting if still running
                }
    }
    private var blockBuilder: ManagedBlockBuilder? = null
    private var witnessBuilder: MultiSigBlockWitnessBuilder? = null

    companion object : KLogging()

    fun stop() {
        logger.debug("stop() - Begin, node: $nodeIndex")
        executor.shutdownNow()
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS) // TODO: [et]: 1000 ms
        maybeRollback()
        logger.debug("stop() - End, node: $nodeIndex")
    }

    private fun <RT> runOpAsync(name: String, op: () -> RT): Promise<RT, Exception> {
        logger.trace("runOpAsync() - $nodeIndex putting job $name on queue")

        val deferred = deferred<RT, Exception>()
        executor.execute {
            try {
                logger.trace("Starting job $name")
                val res = op()
                logger.trace("Finish job $name")
                deferred.resolve(res)
            } catch (e: Exception) {
                logger.debug("Failed job $name", e) // Shouldn't this be at leas WARN?
                deferred.reject(e)
            }
        }

        return deferred.promise
    }

    private fun maybeRollback() {
        logger.trace("maybeRollback() node: $nodeIndex.")
        if (blockBuilder != null) {
            logger.debug("maybeRollback() node: $nodeIndex, blockBuilder is not null.")
        }
        blockBuilder?.rollback()
        blockBuilder = null
        witnessBuilder = null
    }



    /**
     * Adding a block is different from building a block. Here we just want to push this (existing) block into the DB.
     *
     * NOTE:
     * The [BlockchainEngine] creates a new [BlockBuilder] instance for each "addBlock()" call,
     * BUT unlike the other methods in this class "addBlock()" doesn't update the blockBuilder member field.
     *
     * This is why we there is no use setting the [BlockTrace] for this method, we have to send the bTrace instance
     *
     * @param block to be added
     * @param existingBTrace is the trace data of the block we have at current moment. For production this is "null"
     */
    override fun addBlock(block: BlockDataWithWitness, existingBTrace: BlockTrace?): Promise<Unit, Exception> {
        return runOpAsync("addBlock ${block.header.blockRID.toHex()}") {
            addBlockLog("Begin")
            maybeRollback()
            val (theBlockBuilder, exception) = engine.loadUnfinishedBlock(block)
            if (exception != null) {
                addBlockLog("Got error when loading: ${exception.message}" )
                try {
                    theBlockBuilder.rollback()
                } catch (ignore: Exception) {
                }
                throw exception
            } else {
                updateBTrace(existingBTrace, theBlockBuilder.getBTrace())
                theBlockBuilder.commit(block.witness) // No need to set BTrace, because we have it
                addBlockLog("Done commit", theBlockBuilder.getBTrace())
            }
        }
    }

    override fun loadUnfinishedBlock(block: BlockData): Promise<Signature, Exception> {
        return runOpAsync("loadUnfinishedBlock ${block.header.blockRID.toHex()}") {
            maybeRollback()
            val (theBlockBuilder, exception) = engine.loadUnfinishedBlock(block)
            if (exception != null) {
                try {
                    theBlockBuilder.rollback()
                } catch (ignore: Exception) {
                }
                throw exception
            } else {
                blockBuilder = theBlockBuilder
                witnessBuilder = blockBuilder!!.getBlockWitnessBuilder() as MultiSigBlockWitnessBuilder
                witnessBuilder!!.getMySignature()
            }
        }
    }

    override fun commitBlock(signatures: Array<Signature?>): Promise<Unit, Exception> {
        return runOpAsync("commitBlock") {
            // TODO: process signatures
            blockBuilder!!.commit(witnessBuilder!!.getWitness())
            blockBuilder = null
            witnessBuilder = null
        }
    }

    override fun buildBlock(): Promise<Pair<BlockData, Signature>, Exception> {
        return runOpAsync("buildBlock") {
            maybeRollback()
            val (theBlockBuilder, exception) = engine.buildBlock()
            if (exception != null) {
                try {
                    theBlockBuilder.rollback()
                } catch (ignore: Exception) {
                }
                throw UserMistake("Can't build block", exception)
            } else {
                blockBuilder = theBlockBuilder
                witnessBuilder = blockBuilder!!.getBlockWitnessBuilder() as MultiSigBlockWitnessBuilder
                Pair(blockBuilder!!.getBlockData(), witnessBuilder!!.getMySignature())
            }
        }
    }

    override fun verifyBlockSignature(s: Signature): Boolean {
        return if (witnessBuilder != null) {
            try {
                witnessBuilder!!.applySignature(s)
                true
            } catch (e: Exception) {
                logger.debug("Signature invalid", e)
                false
            }
        } else {
            false
        }
    }

    override fun getBlockSignature(blockRID: ByteArray): Promise<Signature, Exception> {
        return blockQueries.getBlockSignature(blockRID)
    }

    override fun getBlockAtHeight(height: Long, includeTransactions: Boolean): Promise<BlockDataWithWitness?, Exception> {
        return blockQueries.getBlockAtHeight(height, includeTransactions)
    }

    // -----------
    // Only for logging
    // -----------

    override fun setBlockTrace(bTrace: BlockTrace) {
        if (this.blockBuilder != null){
            if (this.blockBuilder!!.getBTrace() != null) {
                this.blockBuilder!!.getBTrace()!!.addDataIfMissing(bTrace)
            } else {
                this.blockBuilder!!.setBTrace(bTrace) // use the one we got
            }
        }
    }

    fun updateBTrace(existingBTrace: BlockTrace?, newBlockTrace: BlockTrace?) {
        addBlockLog("About to commit", newBlockTrace)
        if (existingBTrace != null) {
            if (newBlockTrace != null) {
                val newBt = newBlockTrace!!
                existingBTrace.blockRid = newBt.blockRid // Our old BTrace doesn't have the block RID
                newBt.addDataIfMissing(existingBTrace) // Overwrite if doesn't exist
            } else {
                addBlockLog("ERROR why no BTrace?")
            }
        }
    }

    fun addBlockLog(str: String) {
        if (logger.isTraceEnabled) {
            logger.trace("addBlock() -- $str")
        }
    }
    fun addBlockLog(str: String, bTrace: BlockTrace?) {
        if (logger.isTraceEnabled) {
            logger.trace("addBlock() -- $str, from block: $bTrace")
        }
    }
}