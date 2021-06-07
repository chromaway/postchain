// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.multiple_chains

import mu.KLogging
import net.postchain.devtools.ConfigFileBasedIntegrationTest
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.devtools.assertChainStarted
import net.postchain.devtools.assertNodeConnectedWith
import net.postchain.util.NodesTestHelper.selectAnotherRandNode
import org.awaitility.Awaitility.await
import org.awaitility.Duration.ONE_MINUTE
import org.awaitility.Duration.TEN_SECONDS
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import kotlin.test.assertNotNull

open class FullEbftMultipleChainsTestNightly : ConfigFileBasedIntegrationTest() {

    companion object : KLogging()

    private fun strategyOf(nodeId: Int, chainId: Long): OnDemandBlockBuildingStrategy {
        return nodes[nodeId].blockBuildingStrategy(chainId) as OnDemandBlockBuildingStrategy
    }

    protected fun runXNodesWithYTxPerBlock(
            nodesCount: Int,
            blocksCount: Int,
            txPerBlock: Int,
            nodeConfigsFilenames: Array<String>,
            blockchainConfigsFilenames: Array<String>
    ) {

        logger.info {
            "runXNodesWithYTxPerBlock(): " +
                    "nodesCount: $nodesCount, blocksCount: $blocksCount, txPerBlock: $txPerBlock"
        }

        val chains = arrayOf(1L, 2L)

        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))

        // Creating node with two chains
        createMultipleChainNodes(nodesCount, nodeConfigsFilenames, blockchainConfigsFilenames)

        // Asserting all chains are started
        await().atMost(TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { node ->
                        chains.forEach(node::assertChainStarted)
                    }
                }


        // Asserting all chains are connected
        // We don't need to assert all connections, just check some random connections
        if (nodesCount > 1) {
            await().atMost(ONE_MINUTE)
                    .untilAsserted {
                        nodes.forEachIndexed { i, _ ->
                            val randNode = selectAnotherRandNode(i, nodesCount)
                            chains.forEach { chain ->
                                logger.debug("Wait for (node $i, chain $chain) to be connected to node $randNode")
                                nodes[i].assertNodeConnectedWith(chain, nodes[randNode])
                            }
                        }
                    }
        }

        // Enqueueing txs
        var txId = 0
        for (block in 0 until blocksCount) {
            (0 until txPerBlock).forEach { _ ->
                val currentTxId = txId++
                nodes.forEach { node ->
                    chains.forEach { chain ->
                        node.transactionQueue(chain).enqueue(
                                TestTransaction(currentTxId))
                    }
                }
            }

            nodes.indices.forEach { nodeId ->
                chains.forEach { chain ->
                    logger.info { "Node: $nodeId, chain: $chain -> Trigger block" }
                    strategyOf(nodeId, chain).buildBlocksUpTo(block.toLong())
                    logger.info { "Node: $nodeId, chain: $chain -> Await committed" }
                    strategyOf(nodeId, chain).awaitCommitted(block)
                }
            }
        }

        // Assertions
        val expectedHeight = (blocksCount - 1).toLong()
        nodes.forEachIndexed { nodeId, node ->
            chains.forEach { chain ->
                logger.info { "Assertions: node: $nodeId, chain: $chain, expectedHeight: $expectedHeight" }

                val queries = node.blockQueries(chain)

                // Asserting best height equals to 1
                assertEquals(expectedHeight, queries.getBestHeight().get())

                for (height in 0..expectedHeight) {
                    logger.info { "Verifying height $height" }

                    // Asserting uniqueness of block at height
                    val blockRids = queries.getBlockRid(height).get()
                    assertNotNull(blockRids)

                    // Asserting txs count
                    val txs = queries.getBlockTransactionRids(blockRids).get()
                    assertEquals(txPerBlock, txs.size)

                    // Asserting txs content
                    for (tx in 0 until txPerBlock) {
                        val expectedTx = TestTransaction(height.toInt() * txPerBlock + tx)
                        assertArrayEquals(expectedTx.getRID(), txs[tx])

                        val actualTx = queries.getTransaction(txs[tx]).get()!!
                        assertArrayEquals(expectedTx.getRID(), actualTx.getRID())
                        assertArrayEquals(expectedTx.getRawData(), actualTx.getRawData())
                    }
                }
            }
        }
    }

}