// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.integrationtest

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import mu.KLogging
import net.postchain.test.EbftIntegrationTest
import net.postchain.test.OnDemandBlockBuildingStrategy
import net.postchain.test.SingleChainTestNode
import net.postchain.test.testinfra.TestTransaction
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class FullEbftTestNightly : EbftIntegrationTest() {

    companion object : KLogging()

    private fun strategy(node: SingleChainTestNode): OnDemandBlockBuildingStrategy {
        return node
                .getBlockchainInstance()
                .getEngine()
                .getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy
    }

    @Test
    @Parameters(
            "3, 1, 0", "3, 2, 0", "3, 10, 0"
            , "3, 1, 10", "3, 2, 10", "3, 10, 10"
            , "4, 1, 0", "4, 2, 0", "4, 10, 0"
            , "4, 1, 10", "4, 2, 10", "4, 10, 10"
            , "8, 1, 0", "8, 2, 0", "8, 10, 0"
            , "8, 1, 10", "8, 2, 10", "8, 10, 10"
//            , "25, 100, 0"
    )
    fun runXNodesWithYTxPerBlock(nodeCount: Int, blockCount: Int, txPerBlock: Int) {
        configOverrides.setProperty("blockchain.1.blockstrategy", OnDemandBlockBuildingStrategy::class.qualifiedName)
        createEbftNodes(nodeCount)

        var txId = 0
        var statusManager = ebftNodes[0].getBlockchainInstance().statusManager
        for (i in 0 until blockCount) {
            for (tx in 0 until txPerBlock) {
                ebftNodes[statusManager.primaryIndex()]
                        .getBlockchainInstance()
                        .getEngine()
                        .getTransactionQueue()
                        .enqueue(TestTransaction(txId++))
            }
            strategy(ebftNodes[statusManager.primaryIndex()]).triggerBlock()
            ebftNodes.forEach { strategy(it).awaitCommitted(i) }
        }

        val queries = ebftNodes[0].getBlockchainInstance().getEngine().getBlockQueries()
        val referenceHeight = queries.getBestHeight().get()
        ebftNodes.forEach { node ->
            val queries = node.getBlockchainInstance().getEngine().getBlockQueries()
            assertEquals(referenceHeight, queries.getBestHeight().get())

            for (height in 0..referenceHeight) {
                val rids = queries.getBlockRids(height).get()
                assertEquals(1, rids.size)

                val txs = queries.getBlockTransactionRids(rids[0]).get()
                assertEquals(txPerBlock, txs.size)

                for (tx in 0 until txPerBlock) {
                    val expectedTx = TestTransaction((height * txPerBlock + tx).toInt())
                    assertArrayEquals(expectedTx.getRID(), txs[tx])

                    val actualTx = queries.getTransaction(txs[tx]).get()
                    assertArrayEquals(expectedTx.getRID(), actualTx?.getRID())
                    assertArrayEquals(expectedTx.getRawData(), actualTx!!.getRawData())
                }
            }
        }
    }
}
