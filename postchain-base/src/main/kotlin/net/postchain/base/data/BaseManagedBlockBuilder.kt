// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base.data

import mu.KLogging
import net.postchain.base.Storage
import net.postchain.common.TimeLog
import net.postchain.common.toHex
import net.postchain.core.*

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
 */
class BaseManagedBlockBuilder(
        private val eContext: EContext,
        val storage: Storage,
        val blockBuilder: BlockBuilder,
        val beforeCommit: (BlockBuilder) -> Unit,
        val afterCommit: (BlockBuilder) -> Unit
) : ManagedBlockBuilder {
    companion object : KLogging()

    var closed: Boolean = false

    /**
     * Wrapper for blockbuilder operations. Will close current working block for further modifications
     * if an operation fails to execute in full.
     *
     * @param RT type of returned object from called operation (Currently all Unit)
     * @param fn operation to be executed
     * @return whatever [fn] returns
     */
    fun <RT> runOp(fn: () -> RT): RT {
        if (closed)
            throw ProgrammerMistake("Already closed")
        try {
            return fn()
        } catch (e: Exception) {
            rollback()
            throw e
        }
    }

    override fun begin(partialBlockHeader: BlockHeader?) {
        runOp { blockBuilder.begin(partialBlockHeader) }
    }

    override fun appendTransaction(tx: Transaction) {
        runOp { blockBuilder.appendTransaction(tx) }
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
        val exception = storage.withSavepoint(eContext) {
            TimeLog.startSum("BaseManagedBlockBuilder.maybeAppendTransaction().insideSavepoint")
            try {
                blockBuilder.appendTransaction(tx)
            } finally {
                TimeLog.end("BaseManagedBlockBuilder.maybeAppendTransaction().insideSavepoint")
            }
        }
        TimeLog.end("BaseManagedBlockBuilder.maybeAppendTransaction().withSavepoint")
        if (exception != null) {
            logger.info("Failed to append transaction ${tx.getRID().toHex()}", exception)
        }
        return exception
    }

    override fun finalizeBlock() {
        runOp { blockBuilder.finalizeBlock() }
    }

    override fun finalizeAndValidate(blockHeader: BlockHeader) {
        runOp { blockBuilder.finalizeAndValidate(blockHeader) }
    }

    override fun getBlockData(): BlockData {
        return blockBuilder.getBlockData()
    }

    override fun getBlockWitnessBuilder(): BlockWitnessBuilder? {
        if (closed) throw ProgrammerMistake("Already closed")
        return blockBuilder.getBlockWitnessBuilder()
    }

    override fun commit(blockWitness: BlockWitness?) {
        runOp { blockBuilder.commit(blockWitness) }
        closed = true
        beforeCommit(blockBuilder)
        storage.closeWriteConnection(eContext, true)
        afterCommit(blockBuilder)
    }

    override fun rollback() {
        logger.debug("${eContext.nodeID} BaseManagedBlockBuilder.rollback()")
        if (closed) throw ProgrammerMistake("Already closed")
        closed = true
        storage.closeWriteConnection(eContext, false)
    }
}