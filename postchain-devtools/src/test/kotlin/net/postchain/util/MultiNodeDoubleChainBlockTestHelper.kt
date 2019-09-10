package net.postchain.util

import mu.KLogging
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.configurations.GTXTestModule
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.testinfra.TestOneOpGtxTransaction
import net.postchain.gtx.GTXTransaction
import net.postchain.gtx.GTXTransactionFactory
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.assertNodeConnectedWith
import net.postchain.integrationtest.multiple_chains.FullEbftMultipleChainsTestNightly
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Assert
import kotlin.random.Random
import kotlin.test.assertNotNull

open class MultiNodeDoubleChainBlockTestHelper: IntegrationTest()  {

    private val gtxTestModule =  GTXTestModule()
    private val factory1 = GTXTransactionFactory(blockchainRids[1L]!!.hexStringToByteArray(), gtxTestModule, cryptoSystem)
    private val factory2 = GTXTransactionFactory(blockchainRids[2L]!!.hexStringToByteArray(), gtxTestModule, cryptoSystem)
    private val factoryMap = mapOf(
            1L to factory1,
            2L to factory2)

    companion object: KLogging()


    private fun strategyOf(nodeId: Int, chainId: Long): OnDemandBlockBuildingStrategy {
        return nodes[nodeId].blockBuildingStrategy(chainId) as OnDemandBlockBuildingStrategy
    }

    fun runXNodes(
            nodesCount: Int,
            chainList: List<Long>,
            nodeConfigsFilenames: Array<String>,
            blockchainConfigsFilenames: Array<String>
    ) {
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))

        // Creating node with two chains
        logger.debug("---Creating node with two chains ----------------------------")
        createMultipleChainNodes(nodesCount, nodeConfigsFilenames, blockchainConfigsFilenames)

        // Asserting all chains are started
        logger.debug("---Asserting all chains are started -------------------------")
        Awaitility.await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { node ->
                        chainList.forEach(node::assertChainStarted)
                    }
                }

        if (nodes.size > 1) {
            logger.debug("---Asserting all chains are connected -------------------------")
            // We don't need to assert all connections, just check some random connections
            Awaitility.await().atMost(Duration.TEN_SECONDS)
                    .untilAsserted {
                        nodes.forEachIndexed { i, node ->
                            logger.debug("Node $i")
                            var randNode = Random.nextInt(nodesCount)
                            while (randNode == i) {
                                randNode = Random.nextInt(nodesCount) // Cannot be connected to itself, so pic new value
                            }
                            val x = this.nodes[randNode]
                            chainList.forEach { chain ->
                                logger.debug("Wait for (node $i, chain $chain) to be connected to node $randNode")
                                nodes[i].assertNodeConnectedWith(chain, x)
                            }
                        }
                    }
        }


    }

    /**
     * Note that we are enqueing real GTX TXs here.
     *
     * @param blocksCount number of blocks to build
     * @param txPerBlock number of TX in each block
     * @param chainList all BC we will use
     * @param factoryMap a factory per BC
     */
    fun runXNodesWithYTxPerBlock(
            blocksCount: Int,
            txPerBlock: Int,
            chainList: List<Long>
    ): List<GTXTransaction> {

        // Enqueueing txs
        logger.debug("---Enqueueing txs --------------------------------------------")
        val retList = mutableListOf<GTXTransaction>()
        var txId = 0
        for (block in 0 until blocksCount) {
            (0 until txPerBlock).forEach { _ ->
                val currentTxId = txId++
                logger.debug("+++++++++++++++++++++++++++++++++++++++++++")
                logger.debug("++++ block: $block, txId: $txId +++++++")
                logger.debug("+++++++++++++++++++++++++++++++++++++++++++")
                nodes.forEach { node ->
                    chainList.forEach { chain ->
                        logger.debug("++++ block: $block, txId: $txId, node: $node, chain: $chain")
                        val tx = TestOneOpGtxTransaction(factoryMap[chain]!!, currentTxId).getGTXTransaction()
                        retList.add(tx)
                        node.transactionQueue(chain).enqueue(tx)
                    }
                }
            }

            nodes.indices.forEach { nodeId ->
                chainList.forEach { chain ->
                    logger.debug("-------------------------------------------")
                    logger.info { "Node: $nodeId, chain: $chain -> Trigger block" }
                    logger.debug("-------------------------------------------")
                    strategyOf(nodeId, chain).buildBlocksUpTo(block.toLong())
                    logger.debug("-------------------------------------------")
                    logger.info { "Node: $nodeId, chain: $chain -> Await committed" }
                    logger.debug("-------------------------------------------")
                    strategyOf(nodeId, chain).awaitCommitted(block)
                }
            }
        }
        return retList
    }


    fun runXNodesAssertions(
            blocksCount: Int,
            txPerBlock: Int,
            chainList: List<Long>,
            txList: List<GTXTransaction>
    ) {
        logger.debug("---Assertions -------------------------------------------------")
        val txCache = TxCache(txList)
        // Assertions
        val expectedHeight = (blocksCount - 1).toLong()
        nodes.forEachIndexed { nodeId, node ->
            chainList.forEach { chain ->
                logger.info { "Assertions: node: $nodeId, chain: $chain, expectedHeight: $expectedHeight" }

                val queries = node.blockQueries(chain)

                // Asserting best height equals to 1
                Assert.assertEquals(expectedHeight, queries.getBestHeight().get())

                for (height in 0..expectedHeight) {
                    logger.info { "Verifying height $height" }

                    // Asserting uniqueness of block at height
                    val blockRids = queries.getBlockRids(height).get()
                    assertNotNull(blockRids)

                    // Asserting txs count
                    val txs = queries.getBlockTransactionRids(blockRids!!).get()
                    Assert.assertEquals(txPerBlock, txs.size)

                    // Asserting txs content
                    for (tx in 0 until txPerBlock) {
                        val txPos = height.toInt() * txPerBlock + tx
                        val expectedTxRid = txCache.getCachedTxRid(chain.toInt(), chainList.size, height.toInt(), txPerBlock, tx)

                        //val expectedTx = TestTransaction(height.toInt() * txPerBlock + tx)
                        val realTxRid = txs[tx]
                        logger.debug("Real TX RID: ${realTxRid.toHex()}")
                        Assert.assertArrayEquals(expectedTxRid, realTxRid)
                    }
                }
            }
        }
    }
}
