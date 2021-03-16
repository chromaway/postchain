package net.postchain.base

import net.postchain.core.*

class BaseBlockBuildingStrategy_POS130(val configData: BaseBlockchainConfigurationData,
                                val blockchainConfiguration: BlockchainConfiguration,
                                blockQueries: BlockQueries,
                                private val txQueue: TransactionQueue
) : BlockBuildingStrategy {
    private var lastBlockTime: Long
    private var lastTxTime = System.currentTimeMillis()
    private var lastTxSize = 0
    private val strategyData = configData.getBlockBuildingStrategy()
    private val maxBlockTime = strategyData?.get("maxblocktime")?.asInteger() ?: 30000
    private val blockDelay = strategyData?.get("blockdelay")?.asInteger() ?: 100
    private val maxBlockTransactions = strategyData?.get("maxblocktransactions")?.asInteger() ?: 100

    init {
        val height = blockQueries.getBestHeight().get()
        lastBlockTime = if (height == -1L) {
            System.currentTimeMillis()
        } else {
            val blockRID = blockQueries.getBlockRid(height).get()!!
            (blockQueries.getBlockHeader(blockRID).get() as BaseBlockHeader).timestamp
        }
    }

    override fun shouldStopBuildingBlock(bb: BlockBuilder): Boolean {
        val abb = bb as AbstractBlockBuilder
        return abb.transactions.size >= maxBlockTransactions
    }

    override fun blockCommitted(blockData: BlockData) {
        lastBlockTime = (blockData.header as BaseBlockHeader).timestamp
    }

    override fun shouldBuildBlock(): Boolean {
        if (System.currentTimeMillis() - lastBlockTime > maxBlockTime) {
            lastTxSize = 0
            lastTxTime = System.currentTimeMillis()
            return true
        }
        val transactionQueueSize = txQueue.getTransactionQueueSize()
        if (transactionQueueSize > 0) {
            if (transactionQueueSize >= maxBlockTransactions) {
                lastTxSize = 0
                lastTxTime = System.currentTimeMillis()
                return true
            }
            if (transactionQueueSize == lastTxSize && lastTxTime + blockDelay < System.currentTimeMillis()) {
                lastTxSize = 0
                lastTxTime = System.currentTimeMillis()
                return true
            }
            if (transactionQueueSize > lastTxSize) {
                lastTxSize = transactionQueueSize
                lastTxTime = System.currentTimeMillis()
            }
            return false
        }
        return false
    }

}