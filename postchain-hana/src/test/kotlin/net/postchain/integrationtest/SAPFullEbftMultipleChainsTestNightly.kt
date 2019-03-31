package net.postchain.integrationtest

import assertk.assertions.isNotNull
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import mu.KLogging
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.testinfra.TestTransaction
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class SAPFullEbftMultipleChainsTestNightly : IntegrationTest() {

    companion object : KLogging()

    private fun strategyOf(nodeId: Int, chainId: Long): OnDemandBlockBuildingStrategy {
        return nodes[nodeId].blockBuildingStrategy(chainId) as OnDemandBlockBuildingStrategy
    }

    @Test
    @Parameters(
            "1, 0", "2, 0", "10, 0"
            , "1, 1", "2, 1", "10, 1"
            , "1, 10", "2, 10", "10, 10"
    )
    @TestCaseName("[{index}] nodesCount: 1, blocksCount: {0}, txPerBlock: {1}")
    fun runSingleNodeWithYTxPerBlock(blocksCount: Int, txPerBlock: Int) {
        runXNodesWithYTxPerBlock(
                1,
                blocksCount,
                txPerBlock,
                arrayOf(
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/single_node/node0.properties"
                ),
                arrayOf(
                        "/net/postchain/multiple_chains/ebft_nightly/single_node/blockchain_config_1.xml",
                        "/net/postchain/multiple_chains/ebft_nightly/single_node/blockchain_config_2.xml"
                ))
    }

    @Test
    @Parameters(
            "1, 0", "2, 0", "10, 0"
            , "1, 1", "2, 1", "10, 1"
            , "1, 10", "2, 10", "10, 10"
    )
    @TestCaseName("[{index}] nodesCount: 2, blocksCount: {0}, txPerBlock: {1}")
    fun runTwoNodesWithYTxPerBlock(blocksCount: Int, txPerBlock: Int) {
        runXNodesWithYTxPerBlock(
                2,
                blocksCount,
                txPerBlock,
                arrayOf(
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/two_nodes/node0.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/two_nodes/node1.properties"
                ),
                arrayOf(
                        "/net/postchain/multiple_chains/ebft_nightly/two_nodes/blockchain_config_1.xml",
                        "/net/postchain/multiple_chains/ebft_nightly/two_nodes/blockchain_config_2.xml"
                ))
    }

    private fun runXNodesWithYTxPerBlock(
            nodesCount: Int,
            blocksCount: Int,
            txPerBlock: Int,
            nodeConfigsFilenames: Array<String>,
            blockchainConfigsFilenames: Array<String>
    ) {

        SAPFullEbftMultipleChainsTestNightly.logger.info {
            "runXNodesWithYTxPerBlock(): " +
                    "nodesCount: $nodesCount, blocksCount: $blocksCount, txPerBlock: $txPerBlock"
        }

        val chains = arrayOf(1L, 2L)

        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))

        // Creating node with two chains
        createMultipleChainNodes(nodesCount, nodeConfigsFilenames, blockchainConfigsFilenames)

        // Asserting all chains are started
        Awaitility.await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { node ->
                        chains.forEach { chain ->
                            assertk.assert(node.retrieveBlockchain(chain)).isNotNull()
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
                    SAPFullEbftMultipleChainsTestNightly.logger.info { "Node: $nodeId, chain: $chain -> Trigger block" }
                    strategyOf(nodeId, chain).buildBlocksUpTo(block.toLong())
                    SAPFullEbftMultipleChainsTestNightly.logger.info { "Node: $nodeId, chain: $chain -> Await committed" }
                    strategyOf(nodeId, chain).awaitCommitted(block)
                }
            }
        }

        // Assertions
        val expectedHeight = (blocksCount - 1).toLong()
        nodes.forEachIndexed { nodeId, node ->
            chains.forEach { chain ->
                SAPFullEbftMultipleChainsTestNightly.logger.info { "Assertions: node: $nodeId, chain: $chain, expectedHeight: $expectedHeight" }

                val queries = node.blockQueries(chain)

                // Asserting best height equals to 1
                Assert.assertEquals(expectedHeight, queries.getBestHeight().get())

                for (height in 0..expectedHeight) {
                    SAPFullEbftMultipleChainsTestNightly.logger.info { "Verifying height $height" }

                    // Asserting uniqueness of block at height
                    val blockRids = queries.getBlockRids(height).get()
                    Assert.assertEquals(1, blockRids.size)

                    // Asserting txs count
                    val txs = queries.getBlockTransactionRids(blockRids[0]).get()
                    Assert.assertEquals(txPerBlock, txs.size)

                    // Asserting txs content
                    for (tx in 0 until txPerBlock) {
                        val expectedTx = TestTransaction(height.toInt() * txPerBlock + tx)
                        Assert.assertArrayEquals(expectedTx.getRID(), txs[tx])

                        val actualTx = queries.getTransaction(txs[tx]).get()!!
                        Assert.assertArrayEquals(expectedTx.getRID(), actualTx.getRID())
                        Assert.assertArrayEquals(expectedTx.getRawData(), actualTx.getRawData())
                    }
                }
            }
        }
    }
}