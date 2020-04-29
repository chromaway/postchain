// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest

import mu.KLogging
import net.postchain.devtools.ConfigFileBasedIntegrationTest
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.testinfra.TestTransaction
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import kotlin.test.assertNotNull

open class FullEbftTestNightlyCore : ConfigFileBasedIntegrationTest() {

    companion object : KLogging()

    private fun strategy(node: PostchainTestNode): OnDemandBlockBuildingStrategy {
        return node
                .getBlockchainInstance()
                .getEngine()
                .getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy
    }

    protected fun runXNodesWithYTxPerBlockTest(blocksCount: Int, txPerBlock: Int) {
        var txId = 0
        for (i in 0 until blocksCount) {
            for (tx in 0 until txPerBlock) {
                val currentTxId = txId++
                nodes.forEach {
                    it.getBlockchainInstance()
                            .getEngine()
                            .getTransactionQueue()
                            .enqueue(TestTransaction(currentTxId))
                }
            }
            logger.info { "Trigger block" }
            nodes.forEach { strategy(it).buildBlocksUpTo(i.toLong()) }
            logger.info { "Await committed" }
            nodes.forEach { strategy(it).awaitCommitted(i) }
        }

        val queries = nodes[0].getBlockchainInstance().getEngine().getBlockQueries()
        val referenceHeight = queries.getBestHeight().get()
        logger.info { "$blocksCount, refHe: $referenceHeight" }
        nodes.forEach { node ->
            val blockQueries = node.getBlockchainInstance().getEngine().getBlockQueries()
            assertEquals(referenceHeight, queries.getBestHeight().get())

            for (height in 0..referenceHeight) {
                logger.info { "Verifying height $height" }
                val rid = blockQueries.getBlockRid(height).get()
                assertNotNull(rid)

                val txs = blockQueries.getBlockTransactionRids(rid!!).get()
                assertEquals(txPerBlock, txs.size)

                for (tx in 0 until txPerBlock) {
                    val expectedTx = TestTransaction((height * txPerBlock + tx).toInt())
                    assertArrayEquals(expectedTx.getRID(), txs[tx])

                    val actualTx = blockQueries.getTransaction(txs[tx]).get()
                    assertArrayEquals(expectedTx.getRID(), actualTx?.getRID())
                    assertArrayEquals(expectedTx.getRawData(), actualTx!!.getRawData())
                }
            }
        }
    }
}
