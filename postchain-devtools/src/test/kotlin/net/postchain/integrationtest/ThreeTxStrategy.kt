// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest

import mu.KLogging
import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.core.*
import java.util.concurrent.LinkedBlockingQueue

class ThreeTxStrategy(
        val configData: BaseBlockchainConfigurationData,
        val blockchainConfiguration: BlockchainConfiguration,
        blockQueries: BlockQueries,
        private val txQueue: TransactionQueue
) : BlockBuildingStrategy {

    companion object : KLogging()

    private val blocks = LinkedBlockingQueue<BlockData>()
    private var committedHeight = -1
    private val index = -1

    override fun shouldBuildBlock(): Boolean {
        logger.debug { "PNode $index shouldBuildBlock? ${txQueue.getTransactionQueueSize()}" }
        return txQueue.getTransactionQueueSize() >= 3
    }

    override fun shouldStopBuildingBlock(bb: BlockBuilder): Boolean {
        return false
    }

    override fun blockCommitted(blockData: BlockData) {
        blocks.add(blockData)
        logger.debug { "PNode $index committed height ${blocks.size}" }
    }

    fun awaitCommitted(blockHeight: Int) {
        logger.debug { "PNode $index awaiting committed $blockHeight" }
        while (committedHeight < blockHeight) {
            blocks.take()
            committedHeight++
        }
    }
}