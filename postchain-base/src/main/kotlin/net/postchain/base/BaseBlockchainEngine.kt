// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base

import mu.KLogging
import net.postchain.base.data.BaseManagedBlockBuilder
import net.postchain.common.TimeLog
import net.postchain.common.toHex
import net.postchain.core.*
import nl.komponents.kovenant.task
import java.lang.Long.max

val LOG_STATS = true

fun ms(n1: Long, n2: Long): Long {
    return (n2 - n1) / 1000000
}

open class BaseBlockchainEngine(private val bc: BlockchainConfiguration,
                                val s: Storage,
                                private val chainID: Long,
                                private val tq: TransactionQueue,
                                private val useParallelDecoding: Boolean = true
) : BlockchainEngine {
    companion object : KLogging()

    private lateinit var strategy: BlockBuildingStrategy
    private lateinit var blockQueries: BlockQueries
    private var initialized = false
    private var closed = false

    private var _restartHandler: RestartHandler? = null

    override fun setRestartHandler(restartHandler: RestartHandler) {
        _restartHandler = restartHandler
    }


    override fun initializeDB() {
        if (initialized) throw ProgrammerMistake("Engine is already initialized")
        withWriteConnection(s, chainID) { ctx ->
            bc.initializeDB(ctx)
            true
        }
        // BlockQueries should be instantiated only after
        // database is initialized
        blockQueries = bc.makeBlockQueries(s)
        strategy = bc.getBlockBuildingStrategy(blockQueries, tq)
        initialized = true
    }

    override fun getTransactionQueue(): TransactionQueue {
        return tq
    }

    override fun getBlockQueries(): BlockQueries {
        return blockQueries
    }

    override fun getBlockBuildingStrategy(): BlockBuildingStrategy {
        return strategy
    }

    override fun getConfiguration(): BlockchainConfiguration {
        return bc
    }

    override fun shutdown() {
        s.close()
    }

    private fun makeBlockBuilder(): ManagedBlockBuilder {
        if (!initialized) throw ProgrammerMistake("Engine is not initialized yet")
        if (closed) throw ProgrammerMistake("Engine is already closed")
        val ctxt = s.openWriteConnection(chainID)
        val bb = bc.makeBlockBuilder(ctxt)
        return BaseManagedBlockBuilder(ctxt, s, bb) { _bb ->
            val aBB = _bb as AbstractBlockBuilder
            tq.removeAll(aBB.transactions)
            strategy.blockCommitted(_bb.getBlockData())
            // TODO
            /*
            val myConfigurationHeight = BaseConfigurationDataStore.findConfiguration(ctxt, aBB.iBlockData.height)
            val nextConfigurationHeight = BaseConfigurationDataStore.findConfiguration(ctxt, aBB.iBlockData.height + 1)
            if (myConfigurationHeight != nextConfigurationHeight)
                if (_restartHandler != null)
                    _restartHandler!!()*/

        }
    }

    override fun addBlock(block: BlockDataWithWitness) {
        val blockBuilder = loadUnfinishedBlock(block)
        blockBuilder.commit(block.witness)
    }

    fun parLoadUnfinishedBlock(block: BlockData): ManagedBlockBuilder {
        val tStart = System.nanoTime()
        val factory = bc.getTransactionFactory()
        val transactions = block.transactions.map { txData ->
            task {
                val tx = factory.decodeTransaction(txData)
                if (!tx.isCorrect()) throw UserMistake("Transaction is not correct")
                tx
            }
        }

        val blockBuilder = makeBlockBuilder()
        blockBuilder.begin()

        val tBegin = System.nanoTime()
        for (tx in transactions) {
            blockBuilder.appendTransaction(tx.get())
        }
        val tEnd = System.nanoTime()

        blockBuilder.finalizeAndValidate(block.header)
        val tDone = System.nanoTime()

        if (LOG_STATS) {
            val nTransactions = block.transactions.size
            val netRate = (nTransactions * 1000000000L) / max(tEnd - tBegin, 1)
            val grossRate = (nTransactions * 1000000000L) / max(tDone - tStart, 1)
            logger.info("""Loaded block (par), ${nTransactions} transactions, \
                ${ms(tStart, tDone)} ms, ${netRate} net tps, ${grossRate} gross tps"""
            )
        }

        return blockBuilder
    }

    fun seqLoadUnfinishedBlock(block: BlockData): ManagedBlockBuilder {
        val tStart = System.nanoTime()
        val blockBuilder = makeBlockBuilder()
        val factory = bc.getTransactionFactory()
        blockBuilder.begin()

        val tBegin = System.nanoTime()
        for (txData in block.transactions) {
            blockBuilder.appendTransaction(factory.decodeTransaction(txData))
        }
        val tEnd = System.nanoTime()

        blockBuilder.finalizeAndValidate(block.header)
        val tDone = System.nanoTime()

        if (LOG_STATS) {
            val nTransactions = block.transactions.size
            val netRate = (nTransactions * 1000000000L) / (tEnd - tBegin)
            val grossRate = (nTransactions * 1000000000L) / (tDone - tStart)
            logger.info("""Loaded block (seq), ${nTransactions} transactions, \
                ${ms(tStart, tDone)} ms, ${netRate} net tps, ${grossRate} gross tps"""
            )
        }

        return blockBuilder
    }

    override fun loadUnfinishedBlock(block: BlockData): ManagedBlockBuilder {
        return if (useParallelDecoding)
            parLoadUnfinishedBlock(block)
        else
            seqLoadUnfinishedBlock(block)
    }

    override fun buildBlock(): ManagedBlockBuilder {
        TimeLog.startSum("BaseBlockchainEngine.buildBlock().buildBlock")
        val tStart = System.nanoTime()

        val blockBuilder = makeBlockBuilder()
        val abstractBlockBuilder = ((blockBuilder as BaseManagedBlockBuilder).bb as AbstractBlockBuilder)
        blockBuilder.begin()
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
            val tx = tq.takeTransaction()
            TimeLog.end("BaseBlockchainEngine.buildBlock().takeTransaction")
            if (tx != null) {
                logger.info("Appending transaction ${tx.getRID().toHex()}")
                TimeLog.startSum("BaseBlockchainEngine.buildBlock().maybeApppendTransaction")
                val exception = blockBuilder.maybeAppendTransaction(tx)
                TimeLog.end("BaseBlockchainEngine.buildBlock().maybeApppendTransaction")
                if (exception != null) {
                    nRejects += 1
                    tq.rejectTransaction(tx, exception)
                } else {
                    nTransactions += 1
                    // tx is fine, consider stopping
                    if (strategy.shouldStopBuildingBlock(abstractBlockBuilder)) {
                        logger.info("Block size limit is reached")
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
            logger.info("""Block is finalized, ${nTransactions} + ${nRejects} transactions, \
                ${ms(tStart, tDone)} ms, ${netRate} net tps, ${grossRate} gross tps"""
            )
        } else {
            logger.info("Block is finalized")
        }


        return blockBuilder
    }
}