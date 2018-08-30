// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.integrationtest

import net.postchain.api.rest.model.ApiTx
import net.postchain.LegacyTestNode
import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.test.EbftIntegrationTest
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue

class TxForwardingTest: EbftIntegrationTest() {

    fun strat(node: LegacyTestNode): ThreeTxStrategy {
        return node.getModel().getEngine().getBlockBuildingStrategy() as ThreeTxStrategy
    }

    fun tx(id: Int): ApiTx {
        return ApiTx(TestTransaction(id).getRawData().toHex())
    }

    var nodeIndex = 0
    class ThreeTxStrategy(val configData: BaseBlockchainConfigurationData,
                          val blockchainConfiguration: BlockchainConfiguration,
                          blockQueries: BlockQueries, private val txQueue: TransactionQueue): BlockBuildingStrategy {
        val blocks = LinkedBlockingQueue<BlockData>()
        var committedHeight = -1
        val index = -1
        override fun shouldBuildBlock(): Boolean {
            logger.debug { "Node $index shouldBuildBlock? ${txQueue.getTransactionQueueSize()}" }
            return txQueue.getTransactionQueueSize() >= 3
        }

        override fun shouldStopBuildingBlock(bb: BlockBuilder): Boolean {
            return false
        }

        override fun blockCommitted(blockData: BlockData) {
            blocks.add(blockData)
            logger.debug { "Node $index committed height ${blocks.size}" }
        }

        fun awaitCommitted(blockHeight: Int) {
            logger.debug { "Node $index awaiting committed $blockHeight" }
            while (committedHeight < blockHeight) {
                blocks.take()
                committedHeight++
            }
        }
    }

    @Test
    fun testTxNotForwardedIfPrimary() {
        configOverrides.setProperty("blockchain.1.blockstrategy", ThreeTxStrategy::class.java.name)
        createEbftNodes(3)

        ebftNodes[0].getModel().apiModel!!.postTransaction(tx(0))
        ebftNodes[1].getModel().apiModel!!.postTransaction(tx(1))
        ebftNodes[2].getModel().apiModel!!.postTransaction(tx(2))
        strat(ebftNodes[2]).awaitCommitted(0)

        ebftNodes[0].getModel().apiModel!!.postTransaction(tx(3))
        ebftNodes[1].getModel().apiModel!!.postTransaction(tx(4))
        ebftNodes[2].getModel().apiModel!!.postTransaction(tx(5))
        strat(ebftNodes[2]).awaitCommitted(1)

        ebftNodes[0].getModel().apiModel!!.postTransaction(tx(6))
        ebftNodes[1].getModel().apiModel!!.postTransaction(tx(7))
        ebftNodes[2].getModel().apiModel!!.postTransaction(tx(8))
        strat(ebftNodes[2]).awaitCommitted(2)

        val q0 = ebftNodes[2].getModel().getEngine().getBlockQueries()
        for (i in 0..2) {
            val b0 = q0.getBlockAtHeight(i.toLong()).get()
            Assert.assertEquals(3, b0.transactions.size)
        }
    }
}