package net.postchain.integrationtest.multiple_chains

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import mu.KLogging
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.integrationtest.assertChainStarted
import org.awaitility.Awaitility.await
import org.awaitility.Duration.TEN_SECONDS
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

@RunWith(JUnitParamsRunner::class)
class FullEbftMultipleChainsWithReplicasTestNightly : IntegrationTest() {

    companion object : KLogging()

    private fun strategyOf(nodeId: Int, chainId: Long): OnDemandBlockBuildingStrategy {
        return nodes[nodeId].blockBuildingStrategy(chainId) as OnDemandBlockBuildingStrategy
    }

    @Test
    @Parameters("2, 1, 3, 2")
    @TestCaseName("[{index}] nodeCount: {0}, replicaCount: {1}, blockCount: {2}, txPerBlock: {3}")
    fun runFiveNodesWithYTxPerBlock(nodeCount: Int, replicaCount: Int, blockCount: Int, txPerBlock: Int) {
        runXNodesWithReplicasWithYTxPerBlock(
                nodeCount,
                replicaCount,
                blockCount,
                txPerBlock,
                arrayOf(
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/node0.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/node1.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/node2.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/node3.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/node4.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/replica0.properties"
                ),
                arrayOf(
                        "/net/postchain/multiple_chains/ebft_nightly/five_nodes/blockchain_config_1.xml",
                        "/net/postchain/multiple_chains/ebft_nightly/five_nodes/blockchain_config_2.xml"
                ))
    }

    private fun runXNodesWithReplicasWithYTxPerBlock(
            nodeCount: Int,
            replicaCount: Int,
            blockCount: Int,
            txPerBlock: Int,
            nodeConfigsFilenames: Array<String>,
            blockchainConfigsFilenames: Array<String>
    ) {
        logger.info {
            "runXNodesWithReplicasWithYTxPerBlock(): " +
                    "nodeCount: $nodeCount, replicaCount: $replicaCount, blockCount: $blockCount, txPerBlock: $txPerBlock"
        }

        val chains = arrayOf(1L, 2L)
        configOverrides.setProperty("testpeerinfos", createPeerInfosWithReplicas(nodeCount, replicaCount))
        createMultipleChainNodesWithReplicas(nodeCount, replicaCount, nodeConfigsFilenames, blockchainConfigsFilenames)

        // Asserting all chains are started
        await().atMost(TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { node ->
                        chains.forEach(node::assertChainStarted)
                    }
                }

        // Enqueueing txs
        var txId = 0
        for (block in 0 until blockCount) {
            (0 until txPerBlock).forEach { _ ->
                val currentTxId = txId++
                nodes.dropLast(replicaCount).forEach { node ->
                    chains.forEach { chain ->
                        node.transactionQueue(chain).enqueue(TestTransaction(currentTxId))
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
        val expectedHeight = (blockCount - 1).toLong()
        nodes.forEachIndexed { nodeId, node ->
            chains.forEach { chain ->
                logger.info { "Assertions: node: $nodeId, chain: $chain, expectedHeight: $expectedHeight" }

                val queries = node.blockQueries(chain)

                // Asserting best height equals to 1
                assertEquals(expectedHeight, queries.getBestHeight().get())

                for (height in 0..expectedHeight) {
                    logger.info { "Verifying height $height" }

                    // Asserting uniqueness of block at height
                    val blockRid = queries.getBlockRids(height).get()
                    assertNotNull(blockRid)

                    // Asserting txs count
                    val txs = queries.getBlockTransactionRids(blockRid!!).get()
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