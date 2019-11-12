// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base

import mu.KLogging
import net.postchain.base.data.BaseBlockBuilder
import net.postchain.base.data.BaseManagedBlockBuilder
import net.postchain.common.TimeLog
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.devtools.PeerNameHelper.shortBrid
import nl.komponents.kovenant.task
import java.lang.Long.max

const val LOG_STATS = true

fun ms(n1: Long, n2: Long): Long {
    return (n2 - n1) / 1000000
}

open class BaseBlockchainEngine(private val blockchainConfiguration: BlockchainConfiguration,
                                val storage: Storage,
                                private val chainID: Long,
                                private val transactionQueue: TransactionQueue,
                                private val useParallelDecoding: Boolean = true
) : BlockchainEngine {

    companion object : KLogging()

    private lateinit var strategy: BlockBuildingStrategy
    private lateinit var blockQueries: BlockQueries
    private var initialized = false
    private var closed = false
    private var restartHandler: RestartHandler = { false }

    override fun initializeDB() {
        if (initialized) {
            throw ProgrammerMistake("Engine is already initialized")
        }

        logger.debug("Initialize DB - begin")
        withWriteConnection(storage, chainID) { ctx ->
            blockchainConfiguration.initializeDB(ctx)
            true
        }

        // BlockQueries should be instantiated only after
        // database is initialized
        blockQueries = blockchainConfiguration.makeBlockQueries(storage)
        strategy = blockchainConfiguration.getBlockBuildingStrategy(blockQueries, transactionQueue)
        initialized = true
        logger.debug("Initialize DB - end")
    }

    override fun setRestartHandler(handler: RestartHandler) {
        restartHandler = handler
    }

    override fun getTransactionQueue(): TransactionQueue {
        return transactionQueue
    }

    override fun getBlockQueries(): BlockQueries {
        return blockQueries
    }

    override fun getBlockBuildingStrategy(): BlockBuildingStrategy {
        return strategy
    }

    override fun getConfiguration(): BlockchainConfiguration {
        return blockchainConfiguration
    }

    override fun shutdown() {
        closed = true
        storage.close()
    }

    private fun makeBlockBuilder(): ManagedBlockBuilder {
        if (!initialized) throw ProgrammerMistake("Engine is not initialized yet")
        if (closed) throw ProgrammerMistake("Engine is already closed")
        val eContext = storage.openWriteConnection(chainID) // TODO: Close eContext

        return BaseManagedBlockBuilder(eContext, storage, blockchainConfiguration.makeBlockBuilder(eContext), { },
                {
                    val blockBuilder = it as AbstractBlockBuilder
                    transactionQueue.removeAll(blockBuilder.transactions)
                    strategy.blockCommitted(blockBuilder.getBlockData())
                    if (restartHandler()) {
                        closed = true
                    }
                })
    }

    override fun addBlock(block: BlockDataWithWitness) {
        val blockBuilder = loadUnfinishedBlock(block)
        blockBuilder.commit(block.witness)
    }

    override fun loadUnfinishedBlock(block: BlockData): ManagedBlockBuilder {
        return if (useParallelDecoding)
            parallelLoadUnfinishedBlock(block)
        else
            sequentialLoadUnfinishedBlock(block)
    }

    private fun smartDecodeTransaction(txData: ByteArray): Transaction {
        var tx = blockchainConfiguration.getTransactionFactory().decodeTransaction(txData)
        val tx2 = transactionQueue.findTransaction(ByteArrayKey(tx.getRID()))
        if (tx2 != null && tx2.getHash().contentEquals(tx.getHash())) {
            // if transaction is identical (has same hash) then use transaction
            // from queue, which is already verified
            tx = tx2
        }
        if (!tx.isCorrect()) throw UserMistake("Transaction is not correct")
        return tx
    }

    private fun parallelLoadUnfinishedBlock(block: BlockData): ManagedBlockBuilder {
        val tStart = System.nanoTime()
        val transactions = block.transactions.map { txData ->
            task { smartDecodeTransaction(txData) }
        }

        val blockBuilder = makeBlockBuilder()
        if (blockBuilder is BaseBlockBuilder) {
            blockBuilder.validateMaxBlockTransactions()
        }
        blockBuilder.begin(block.header)

        val tBegin = System.nanoTime()
        transactions.forEach { blockBuilder.appendTransaction(it.get()) }
        val tEnd = System.nanoTime()

        blockBuilder.finalizeAndValidate(block.header)
        val tDone = System.nanoTime()

        if (LOG_STATS) {
            val nTransactions = block.transactions.size
            val netRate = (nTransactions * 1000000000L) / max(tEnd - tBegin, 1)
            val grossRate = (nTransactions * 1000000000L) / max(tDone - tStart, 1)
            logger.info("""Loaded block (par), $nTransactions transactions, \
                ${ms(tStart, tDone)} ms, $netRate net tps, $grossRate gross tps"""
            )
        }

        return blockBuilder
    }

    private fun sequentialLoadUnfinishedBlock(block: BlockData): ManagedBlockBuilder {
        val tStart = System.nanoTime()
        val blockBuilder = makeBlockBuilder()
        if (blockBuilder is BaseBlockBuilder) {
            blockBuilder.validateMaxBlockTransactions()
        }
        blockBuilder.begin(block.header)

        val tBegin = System.nanoTime()
        block.transactions.forEach {
            blockBuilder.appendTransaction(
                    smartDecodeTransaction(it)
            )
        }
        val tEnd = System.nanoTime()

        blockBuilder.finalizeAndValidate(block.header)
        val tDone = System.nanoTime()

        if (LOG_STATS) {
            val nTransactions = block.transactions.size
            val netRate = (nTransactions * 1000000000L) / (tEnd - tBegin)
            val grossRate = (nTransactions * 1000000000L) / (tDone - tStart)
            logger.info("""Loaded block (seq), $nTransactions transactions, \
                ${ms(tStart, tDone)} ms, $netRate net tps, $grossRate gross tps"""
            )
        }

        return blockBuilder
    }

    override fun buildBlock(): ManagedBlockBuilder {
        TimeLog.startSum("BaseBlockchainEngine.buildBlock().buildBlock")
        val tStart = System.nanoTime()

        val blockBuilder = makeBlockBuilder()
        blockBuilder.begin(null)
        val abstractBlockBuilder = ((blockBuilder as BaseManagedBlockBuilder).blockBuilder as AbstractBlockBuilder)
        val tBegin = System.nanoTime()

        // TODO Potential problem: if the block fails for some reason,
        // the transaction queue is gone. This could potentially happen
        // during a revolt. We might need a "transactional" tx queue...

        TimeLog.startSum("BaseBlockchainEngine.buildBlock().appendtransactions")
        var nTransactions = 0
        var nRejects = 0

        while (true) {
            logger.debug("Checking transaction queue")
            TimeLog.startSum("BaseBlockchainEngine.buildBlock().takeTransaction")
            val tx = transactionQueue.takeTransaction()
            TimeLog.end("BaseBlockchainEngine.buildBlock().takeTransaction")
            if (tx != null) {
                logger.debug("Appending transaction ${tx.getRID().toHex()}")
                TimeLog.startSum("BaseBlockchainEngine.buildBlock().maybeApppendTransaction")
                val exception = blockBuilder.maybeAppendTransaction(tx)
                TimeLog.end("BaseBlockchainEngine.buildBlock().maybeApppendTransaction")
                if (exception != null) {
                    nRejects += 1
                    transactionQueue.rejectTransaction(tx, exception)
                } else {
                    nTransactions += 1
                    // tx is fine, consider stopping
                    if (strategy.shouldStopBuildingBlock(abstractBlockBuilder)) {
                        logger.debug("Block size limit is reached")
                        break
                    }
                }
            } else { // tx == null
                break
            }
        }

        TimeLog.end("BaseBlockchainEngine.buildBlock().appendtransactions")

        val tEnd = System.nanoTime()
        blockBuilder.finalizeBlock()
        val tDone = System.nanoTime()

        TimeLog.end("BaseBlockchainEngine.buildBlock().buildBlock")

        if (LOG_STATS) {
            val netRate = (nTransactions * 1000000000L) / (tEnd - tBegin)
            val grossRate = (nTransactions * 1000000000L) / (tDone - tStart)
            logger.info("Chain: ${shortBrid(blockchainConfiguration.blockchainRID)}: " +
                    "Block is finalized: accepted tx: $nTransactions, rejected tx: $nRejects; " +
                    "${ms(tStart, tDone)} ms, $netRate net tps, $grossRate gross tps")
        } else {
            logger.info("Block is finalized")
        }

        return blockBuilder
    }
}