// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.integrationtest

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import mu.KLogging
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.testinfra.TestTransaction
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

open class FullEbftTestNightlyCore : IntegrationTest() {

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
            val queries = node.getBlockchainInstance().getEngine().getBlockQueries()
            assertEquals(referenceHeight, queries.getBestHeight().get())

            for (height in 0..referenceHeight) {
                logger.info { "Verifying height $height" }
                val rid = queries.getBlockRids(height).get()
                assertNotNull(rid)

                val txs = queries.getBlockTransactionRids(rid!!).get()
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
