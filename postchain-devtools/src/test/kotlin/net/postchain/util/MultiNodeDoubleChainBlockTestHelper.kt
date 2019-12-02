package net.postchain.util

import mu.KLogging
import net.postchain.base.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.configurations.GTXTestModule
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.TxCache
import net.postchain.devtools.testinfra.TestOneOpGtxTransaction
import net.postchain.devtools.utils.configuration.NodeNameWithBlockchains
import net.postchain.gtx.GTXTransactionFactory
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.assertNodeConnectedWith
import net.postchain.util.NodesTestHelper.selectAnotherRandNode
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Assert
import kotlin.test.assertNotNull

open class MultiNodeDoubleChainBlockTestHelper : IntegrationTest() {

    private val gtxTestModule = GTXTestModule()
    private val factoryMap = mutableMapOf<Long, GTXTransactionFactory>() // Will be filled up after node start and used during TX creation.
    companion object : KLogging()


    private fun strategyOf(nodeId: Int, chainId: Long): OnDemandBlockBuildingStrategy {
        return nodes[nodeId].blockBuildingStrategy(chainId) as OnDemandBlockBuildingStrategy
    }

    /**
     * Starts the nodes and the same amount of chains for every node
     *
     * @param nodesCount = number of nodes
     * @param chainList chains all nodes are using
     * @param nodeConfigsFilenames = filenames for node conf
     * @param blockchainConfigsFilenames = filenames for bc conf
     */
    fun runXNodes(
            nodesCount: Int,
            chainList: List<Long>,
            nodeConfigsFilenames: Array<String>,
            blockchainConfigsFilenames: Array<String>
    ) {
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))

        // Creating node with two chains
        logger.debug("---Creating node with many chains ----------------------------")
        createMultipleChainNodes(nodesCount, nodeConfigsFilenames, blockchainConfigsFilenames)

        // Asserting all chains are started
        logger.debug("---Asserting all chains are started -------------------------")
        Awaitility.await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { node ->
                        chainList.forEach(node::assertChainStarted)
                    }
                }

        // At this point we know the BC RID so go ahead and create the TX factories
        chainList.forEach {
            val bcRid = nodes[0].getBlockchainRid(it)!! // Doesn't matter what node we take so I'm  just picking the first
            factoryMap[it] = GTXTransactionFactory(bcRid, gtxTestModule, cryptoSystem)
        }

        if (nodes.size > 1) {
            logger.debug("---Asserting all chains are connected -------------------------")
            // We don't need to assert all connections, just check some random connections
            Awaitility.await().atMost(Duration.TEN_SECONDS)
                    .untilAsserted {
                        nodes.forEachIndexed { i, _ ->
                            logger.debug("Node $i")
                            val randNode = selectAnotherRandNode(i, nodesCount)
                            chainList.forEach { chain ->
                                logger.debug("Wait for (node $i, chain $chain) to be connected to node $randNode")
                                nodes[i].assertNodeConnectedWith(chain, nodes[randNode])
                            }
                        }
                    }
        }


    }

    /**
     * Starts the nodes with the given number of chains different for each node
     *
     * @param nodesCount = number of nodes
     * @param nodeNameWithBlockchainsArr = what chains each node has
     * @param chainsInCommon = what chains are in common for all nodes
     */
    fun runXNodesWithDifferentNumberOfChainsPerNode(
            nodesCount: Int,
            nodeNameWithBlockchainsArr: Array<NodeNameWithBlockchains>,
            chainsInCommon: List<Long>

    ) {
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))

        // Creating node with two chains
        logger.debug("---Creating node with custom number of chains ----------------------------")
        createMultipleChainNodesWithVariableNumberOfChains(nodesCount, nodeNameWithBlockchainsArr)

        // Asserting all chains are started
        // (This is a bit more complicated since we have different chains per node)
        logger.debug("---Asserting all chains are started -------------------------")
        Awaitility.await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEachIndexed { i, node ->
                        nodeNameWithBlockchainsArr[i].getChainIds().forEach { node::assertChainStarted }
                    }
                }

        // At this point we know the BC RID so go ahead and create the TX factories
        chainsInCommon.forEach {
            val bcRid = nodes[0].getBlockchainRid(it)!! // I'm  just picking it from the first node I find.
            factoryMap[it] = GTXTransactionFactory(bcRid, gtxTestModule, cryptoSystem)
        }


        if (nodes.size > 1) {
            logger.debug("---Asserting all chains are connected -------------------------")
            // We don't need to assert all connections, just check some random connections
            Awaitility.await().atMost(Duration.TEN_SECONDS)
                    .untilAsserted {
                        nodes.forEachIndexed { i, node ->
                            logger.debug("Assert connection for Node: $i")

                            // TODO: NOTE: Here we assume that all nodes are connected EVEN THOUGH they might not have a common chain
                            val randNode = selectAnotherRandNode(i, nodesCount)
                            chainsInCommon.forEach { chain ->
                                logger.debug("Wait for (node $i, chain $chain) to be connected to node $randNode")
                                nodes[i].assertNodeConnectedWith(chain, nodes[randNode])
                            }
                        }
                    }
        }
    }


    /**
     * Note that we are enqueueing real GTX TXs here.
     *
     * @param blocksCount number of blocks to build
     * @param txPerBlock number of TX in each block
     * @param chainList all BC we will use
     * @param nodeNameWithBlockchainsArr = null if all nodes have the same chains, else with a value
     */
    fun runXNodesWithYTxPerBlock(
            blocksCount: Int,
            txPerBlock: Int,
            chainList: List<Long>,
            txCache: TxCache,
            nodeNameWithBlockchainsArr: Array<NodeNameWithBlockchains>? = null
    ) {

        // Enqueueing txs
        logger.debug("---Enqueueing txs --------------------------------------------")
        var txId = 0
        for (block in 0 until blocksCount) {
            for (blockIndex in 0 until txPerBlock) {
                val currentTxId = txId++
                logger.debug("+++++++++++++++++++++++++++++++++++++++++++")
                logger.debug("++++ block: $block, txId: $txId +++++++")
                logger.debug("+++++++++++++++++++++++++++++++++++++++++++")
                nodes.forEachIndexed { i, node ->

                    if (nodeNameWithBlockchainsArr != null) {
                        nodeNameWithBlockchainsArr[i].getChainIds().forEach { chain ->
                            enqueueTx(chain, currentTxId, txCache, block, blockIndex, node)
                        }
                    } else {
                        chainList.forEach { chain ->
                            enqueueTx(chain, currentTxId, txCache, block, blockIndex, node)
                        }
                    }
                }
            }

            nodes.indices.forEach { nodeId ->
                if (nodeNameWithBlockchainsArr != null) {
                    nodeNameWithBlockchainsArr[nodeId].getWritableChainIds().forEach { chain ->
                        buildBlocks(nodeId, chain, block) // The block we can build ourselves
                    }
                    nodeNameWithBlockchainsArr[nodeId].getReadOnlyChainIds().forEach { chain ->
                        // The blocks we must fetch form the node
                    }
                } else {
                    chainList.forEach { chain ->
                        buildBlocks(nodeId, chain, block)
                    }
                }
            }
        }
    }

    private fun buildBlocks(nodeId: Int, chain: Long, block: Int) {
        logger.debug("-------------------------------------------")
        logger.info { "Node: $nodeId, chain: $chain -> Trigger block" }
        logger.debug("-------------------------------------------")
        strategyOf(nodeId, chain).buildBlocksUpTo(block.toLong())
        logger.debug("-------------------------------------------")
        logger.info { "Node: $nodeId, chain: $chain -> Await committed" }
        logger.debug("-------------------------------------------")
        strategyOf(nodeId, chain).awaitCommitted(block)
    }

    private fun enqueueTx(chain: Long, currentTxId: Int, txCache: TxCache, height: Int, blockIndex: Int, node: PostchainTestNode) {
        logger.debug("++++ block-height: $height, block-index: $blockIndex, node: $node, chain: $chain")
        val tx = TestOneOpGtxTransaction(factoryMap[chain]!!, currentTxId).getGTXTransaction()
        txCache.addTx(tx, chain.toInt(), height, blockIndex)
        node.transactionQueue(chain).enqueue(tx)
    }


    fun runXNodesAssertions(
            blocksCount: Int,
            txPerBlock: Int,
            chainList: List<Long>,
            txCache: TxCache,
            nodeNameWithBlockchainsArr: Array<NodeNameWithBlockchains>? = null
    ) {
        logger.debug("---Assertions -------------------------------------------------")
        // Assertions

        val expectedHeight = (blocksCount - 1).toLong()
        //logger.debug("Nr of nodes: ${nodes.size}, total nr of blocks: $blocksCount => expected height: $expectedHeight")

        nodes.forEachIndexed { nodeId, node ->
            if (nodeNameWithBlockchainsArr != null) {
                val chains = nodeNameWithBlockchainsArr[nodeId].getChainIds()
                chains.forEach { chain ->
                    assertChainForNode(nodeId, chain, expectedHeight, node, txPerBlock, txCache)
                }

            } else {
                chainList.forEach { chain ->
                    assertChainForNode(nodeId, chain, expectedHeight, node, txPerBlock, txCache)
                }
            }
        }
    }

    private fun assertChainForNode(nodeId: Int, chain: Long, expectedHeight: Long, node: PostchainTestNode, txPerBlock: Int, txCache: TxCache) {
        logger.info { "Assertions: node: $nodeId, chain: $chain, expectedHeight: $expectedHeight" }

        val queries = node.blockQueries(chain)

        // Asserting best height equals to expected
        val best = queries.getBestHeight().get()
        Assert.assertEquals(expectedHeight, best)

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
                //val txPos = height.toInt() * txPerBlock + tx
                //val expectedTxRid = txCache.getCachedTxRid(chain.toInt(), numberOfChains, height.toInt(), txPerBlock, tx)
                val expectedTxRid = txCache.getCachedTxRid(chain.toInt(), height.toInt(), tx)

                //val expectedTx = TestTransaction(height.toInt() * txPerBlock + tx)
                val realTxRid = txs[tx]
                logger.debug("Real TX RID: ${realTxRid.toHex()}")
                Assert.assertArrayEquals(expectedTxRid, realTxRid)
            }
        }
    }
}
