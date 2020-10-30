// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import mu.KLogging
import net.postchain.base.data.BaseManagedBlockBuilder
import net.postchain.base.gtv.BlockHeaderData
import net.postchain.common.TimeLog
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.debug.BlockchainProcessName
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvDecoder
import nl.komponents.kovenant.task
import java.lang.Long.max

const val LOG_STATS = true

open class BaseBlockchainEngine(
        private val processName: BlockchainProcessName,
        private val blockchainConfiguration: BlockchainConfiguration,
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

    override fun initialize() {
        if (initialized) {
            throw ProgrammerMistake("Engine is already initialized")
        }
        blockQueries = blockchainConfiguration.makeBlockQueries(storage)
        strategy = blockchainConfiguration.getBlockBuildingStrategy(blockQueries, transactionQueue)
        initialized = true
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
        val (blockBuilder, exception) = loadUnfinishedBlock(block)
        blockBuilder.commit(block.witness)
    }

    override fun loadUnfinishedBlock(block: BlockData): Pair<ManagedBlockBuilder, Exception?> {
        return if (useParallelDecoding)
            parallelLoadUnfinishedBlock(block)
        else
            sequentialLoadUnfinishedBlock(block)
    }

    private fun smartDecodeTransaction(txData: ByteArray): Transaction {
        var tx = blockchainConfiguration.getTransactionFactory().decodeTransaction(txData)
        val enqueuedTx = transactionQueue.findTransaction(ByteArrayKey(tx.getRID()))
        if (enqueuedTx != null && enqueuedTx.getHash().contentEquals(tx.getHash())) {
            // if transaction is identical (has same hash) then use transaction
            // from queue, which is already verified
            tx = enqueuedTx
        }

        return if (tx.isCorrect()) tx
        else throw UserMistake("Transaction is not correct")
    }

    private fun sequentialLoadUnfinishedBlock(block: BlockData): Pair<ManagedBlockBuilder, Exception?> {
        return loadUnfinishedBlockImpl(block) { txs ->
            txs.map { smartDecodeTransaction(it) }
        }
    }

    private fun parallelLoadUnfinishedBlock(block: BlockData): Pair<ManagedBlockBuilder, Exception?> {
        return loadUnfinishedBlockImpl(block) { txs ->
            val txsLazy = txs.map { tx ->
                task { smartDecodeTransaction(tx) }
            }

            txsLazy.map { it.get() }
        }
    }

    private fun loadUnfinishedBlockImpl(
            block: BlockData,
            transactionsDecoder: (List<ByteArray>) -> List<Transaction>
    ): Pair<ManagedBlockBuilder, Exception?> {

        val grossStart = System.nanoTime()
        val blockBuilder = makeBlockBuilder()
        var exception: Exception? = null

        try {
            blockBuilder.begin(block.header)

            val netStart = System.nanoTime()
            val decodedTxs = transactionsDecoder(block.transactions)
            decodedTxs.forEach(blockBuilder::appendTransaction)
            val netEnd = System.nanoTime()

            blockBuilder.finalizeAndValidate(block.header)
            val grossEnd = System.nanoTime()

            if (LOG_STATS) {
                val prettyBlockHeader = prettyBlockHeader(
                        block.header, block.transactions.size, 0, grossStart to grossEnd, netStart to netEnd)
                logger.info("$processName: Loaded block: $prettyBlockHeader")
            }

        } catch (e: Exception) {
            exception = e
        }

        return blockBuilder to exception
    }

    override fun buildBlock(): Pair<ManagedBlockBuilder, Exception?> {
        TimeLog.startSum("BaseBlockchainEngine.buildBlock().buildBlock")
        val grossStart = System.nanoTime()

        val blockBuilder = makeBlockBuilder()
        var exception: Exception? = null

        try {
            blockBuilder.begin(null)
            val abstractBlockBuilder = ((blockBuilder as BaseManagedBlockBuilder).blockBuilder as AbstractBlockBuilder)
            val netStart = System.nanoTime()

            // TODO Potential problem: if the block fails for some reason,
            // the transaction queue is gone. This could potentially happen
            // during a revolt. We might need a "transactional" tx queue...

            TimeLog.startSum("BaseBlockchainEngine.buildBlock().appendtransactions")
            var acceptedTxs = 0
            var rejectedTxs = 0

            while (true) {
                logger.debug("$processName: Checking transaction queue")
                TimeLog.startSum("BaseBlockchainEngine.buildBlock().takeTransaction")
                val tx = transactionQueue.takeTransaction()
                TimeLog.end("BaseBlockchainEngine.buildBlock().takeTransaction")
                if (tx != null) {
                    logger.debug("$processName: Appending transaction ${tx.getRID().toHex()}")
                    TimeLog.startSum("BaseBlockchainEngine.buildBlock().maybeApppendTransaction")
                    if (tx.isSpecial()) {
                        rejectedTxs++
                        continue
                    }
                    val exception = blockBuilder.maybeAppendTransaction(tx)
                    TimeLog.end("BaseBlockchainEngine.buildBlock().maybeApppendTransaction")
                    if (exception != null) {
                        rejectedTxs++
                        transactionQueue.rejectTransaction(tx, exception)
                    } else {
                        acceptedTxs++
                        // tx is fine, consider stopping
                        if (strategy.shouldStopBuildingBlock(abstractBlockBuilder)) {
                            logger.debug("$processName: Block size limit is reached")
                            break
                        }
                    }
                } else { // tx == null
                    break
                }
            }

            TimeLog.end("BaseBlockchainEngine.buildBlock().appendtransactions")

            val netEnd = System.nanoTime()
            val blockHeader = blockBuilder.finalizeBlock()
            val grossEnd = System.nanoTime()

            TimeLog.end("BaseBlockchainEngine.buildBlock().buildBlock")

            if (LOG_STATS) {
                val prettyBlockHeader = prettyBlockHeader(
                        blockHeader, acceptedTxs, rejectedTxs, grossStart to grossEnd, netStart to netEnd)
                logger.info("$processName: Block is finalized: $prettyBlockHeader")
            } else {
                logger.info("$processName: Block is finalized")
            }

        } catch (e: Exception) {
            exception = e
        }

        return blockBuilder to exception
    }

    private fun prettyBlockHeader(
            blockHeader: BlockHeader,
            acceptedTxs: Int,
            rejectedTxs: Int,
            gross: Pair<Long, Long>,
            net: Pair<Long, Long>
    ): String {

        val grossRate = (acceptedTxs * 1_000_000_000L) / max(gross.second - gross.first, 1)
        val netRate = (acceptedTxs * 1_000_000_000L) / max(net.second - net.first, 1)
        val grossTimeMs = (gross.second - gross.first) / 1_000_000

        val gtvBlockHeader = GtvDecoder.decodeGtv(blockHeader.rawData)
        val blockHeaderData = BlockHeaderData.fromGtv(gtvBlockHeader as GtvArray)

        return "$grossTimeMs ms" +
                ", $netRate net tps" +
                ", $grossRate gross tps" +
                ", height: ${blockHeaderData.gtvHeight.asInteger()}" +
                ", accepted txs: $acceptedTxs" +
                ", rejected txs: $rejectedTxs" +
                ", root-hash: ${blockHeaderData.getMerkleRootHash().toHex()}" +
                ", block-rid: ${blockHeader.blockRID.toHex()}" +
                ", prev-block-rid: ${blockHeader.prevBlockRID.toHex()}"
    }

}