// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

import mu.KLogging
import net.postchain.base.Storage
import net.postchain.common.TimeLog
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.debug.BlockTrace

/**
 * Wrapper around BlockBuilder providing more control over the process of building blocks,
 * with checks to see if current working block has been commited or not, and rolling back
 * database state in case some operation fails
 *
 * @property eContext Connection context including blockchain and node identifiers
 * @property storage For database access
 * @property blockBuilder The base block builder
 * @property onCommit Clean-up function to be called when block has been commited
 * @property closed Boolean for if block is open to further modifications and queries. It is closed if
 * an operation fails to execute in full or if a witness is created and the block commited.
 *
 * NOTE: Re logging
 * Looks like this class used to do too much logging, so now everything has been scaled down one notch
 * (debug -> trace, etc). IMO this is better than blocking the logging from YAML (which might be hard to remember)
 */
class BaseManagedBlockBuilder(
        private val eContext: EContext,
        val storage: Storage,
        val blockBuilder: BlockBuilder,
        val beforeCommit: (BlockBuilder) -> Unit,
        val afterCommit: (BlockBuilder) -> Unit
) : ManagedBlockBuilder {
    companion object : KLogging()

    private var closed = false

    var blocTrace: BlockTrace? = null // Only for logging, remains "null" unless TRACE

    /**
     * Wrapper for blockbuilder operations. Will close current working block for further modifications
     * if an operation fails to execute in full.
     *
     * @param RT type of returned object from called operation (Currently all Unit)
     * @param fn operation to be executed
     * @return whatever [fn] returns
     */
    private fun <RT> runOpSafely(fn: () -> RT): RT {
        if (closed) throw ProgrammerMistake("Already closed")

        try {
            return fn()
        } catch (e: Exception) {
            rollback()
            throw e
        }
    }

    override fun begin(partialBlockHeader: BlockHeader?) {
        runOpSafely { blockBuilder.begin(partialBlockHeader) }
    }

    override fun appendTransaction(tx: Transaction) {
        runOpSafely { blockBuilder.appendTransaction(tx) }
    }

    /**
     * Append transaction as long as everything is OK. withSavepoint will roll back any potential changes
     * to the database state if appendTransaction fails to complete
     *
     * @param tx Transaction to be added to the current working block
     * @return exception if error occurs
     */
    override fun maybeAppendTransaction(tx: Transaction): Exception? {
        TimeLog.startSum("BaseManagedBlockBuilder.maybeAppendTransaction().withSavepoint")

        val action = {
            TimeLog.startSum("BaseManagedBlockBuilder.maybeAppendTransaction().insideSavepoint")
            try {
                blockBuilder.appendTransaction(tx)
            } finally {
                TimeLog.end("BaseManagedBlockBuilder.maybeAppendTransaction().insideSavepoint")
            }
        }

        val exception = if (storage.isSavepointSupported()) {
            storage.withSavepoint(eContext, action).also {
                if (it != null) {
                    logger.info("Failed to append transaction ${tx.getRID().toHex()}", it)
                }
            }

        } else {
            action()
            null
        }

        TimeLog.end("BaseManagedBlockBuilder.maybeAppendTransaction().withSavepoint")
        return exception
    }

    override fun finalizeBlock(): BlockHeader {
        return runOpSafely { blockBuilder.finalizeBlock() }
    }

    override fun finalizeAndValidate(blockHeader: BlockHeader) {
        runOpSafely { blockBuilder.finalizeAndValidate(blockHeader) }
    }

    override fun getBlockData(): BlockData {
        return blockBuilder.getBlockData()
    }

    override fun getBlockWitnessBuilder(): BlockWitnessBuilder? {
        if (closed) throw ProgrammerMistake("Already closed")
        return blockBuilder.getBlockWitnessBuilder()
    }

    override fun commit(blockWitness: BlockWitness) {
        commitLog("Start")
        getOrBuildBlockTrace()

        synchronized(storage) {
            if (!closed) {
                commitLog("Got lock")
                beforeCommit(blockBuilder)
                runOpSafely { blockBuilder.commit(blockWitness) }
                storage.closeWriteConnection(eContext, true)
                closed = true
                commitLog("Do after commit")
                afterCommit(blockBuilder)
            }
        }

        commitLog("End")
    }

    override fun rollback() {
        rollbackDebugLog("Start")
        synchronized(storage) {
            if (!closed) {
                rollbackLog("Got lock")
                storage.closeWriteConnection(eContext, false)
                closed = true
            }
        }
        rollbackDebugLog("End")
    }


    // --------------------------------

    // Only used during logging
    override fun getBTrace(): BlockTrace? {
        return blocTrace
    }

    // Only used during logging
    override fun setBTrace(newBlockDebug: BlockTrace) {
        if (blocTrace == null) {
            blocTrace = newBlockDebug
        } else {
            // Update existing object with missing info
            blocTrace!!.addDataIfMissing(newBlockDebug)
        }
    }

    /**
     * We are a "[BlockBuilder]" but we are also decorating the "real" [BlockBuilder] so
     * the inner one should get the [BlockTrace] data from us.
     */
    private fun getOrBuildBlockTrace() {
        if (logger.isTraceEnabled) {
            if (getBTrace() != null) {
                var inner = blockBuilder.getBTrace()
                if (inner != null) {
                    inner.addDataIfMissing(getBTrace())
                } else {
                    blockBuilder.setBTrace(getBTrace()!!)
                }
            }
        }
    }

    private fun commitLog(str: String) {
        if (logger.isTraceEnabled) {
            logger.trace("${eContext.chainID} commit() -- $str, from block: ${getBTrace()}")
        }
    }

    private fun rollbackLog(str: String) {
        if (logger.isTraceEnabled) {
            logger.trace("${eContext.chainID} rollback() -- $str, from block: ${getBTrace()}")
        }
    }

    private fun rollbackDebugLog(str: String) {
        if (logger.isDebugEnabled) {
            logger.debug("${eContext.chainID} rollback() -- $str")
        }
        if (logger.isTraceEnabled) {
            logger.trace("${eContext.chainID} rollback() -- $str, from block: ${getBTrace()}")
        }
    }

}