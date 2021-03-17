// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import net.postchain.core.*

class BaseBlockBuildingStrategy(val configData: BaseBlockchainConfigurationData,
                                val blockchainConfiguration: BlockchainConfiguration,
                                blockQueries: BlockQueries,
                                private val txQueue: TransactionQueue
) : BlockBuildingStrategy {
    private var lastBlockTime: Long
    private var firstTxTime = 0L
    private val strategyData = configData.getBlockBuildingStrategy()
    private val maxBlockTime = strategyData?.get("maxblocktime")?.asInteger() ?: 30000
    private val maxBlockTransactions = strategyData?.get("maxblocktransactions")?.asInteger() ?: 100
    private val maxTxDelay = strategyData?.get("maxtxdelay")?.asInteger() ?: 1000
    private val minInterBlockInterval = strategyData?.get("mininterblockinterval")?.asInteger() ?: 25

    init {
        val height = blockQueries.getBestHeight().get()
        lastBlockTime = if (height == -1L) {
            0
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
        firstTxTime = 0
    }

    override fun shouldBuildBlock(): Boolean {
        val now = System.currentTimeMillis()

        if (now - lastBlockTime > maxBlockTime) return true
        if (now - lastBlockTime < minInterBlockInterval) return false
        if (firstTxTime > 0 && now - firstTxTime > maxTxDelay) return true

        val transactionQueueSize = txQueue.getTransactionQueueSize()
        if (transactionQueueSize >= maxBlockTransactions) return true
        if (firstTxTime == 0L && transactionQueueSize > 0) {
            firstTxTime = now
            return false
        }
        return false
    }

}