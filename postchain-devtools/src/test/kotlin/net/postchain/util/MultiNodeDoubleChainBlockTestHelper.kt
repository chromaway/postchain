package net.postchain.util

import mu.KLogging
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.configurations.GTXTestModule
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.TxCache
import net.postchain.devtools.testinfra.TestOneOpGtxTransaction
import net.postchain.devtools.utils.configuration.NodeNameWithBlockchains
import net.postchain.devtools.utils.configuration.SystemSetup
import net.postchain.gtx.GTXTransactionFactory
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.assertNodeConnectedWith
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Assert
import kotlin.random.Random
import kotlin.test.assertNotNull

/**
 * Extends [IntegrationTest] with extra functions relevant for tests running multiple chains.
 *
 * Note 1: We are using (real) [GTXTransaction] here, not some mock transaction designed for tests
 * Note 2: We currently only handle two chains // TODO
 */
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
     * Starts the nodes with the given number of chains different for each node
     *
     * @param systemSetup holds the detailed description of the system
     */
    fun runXNodesWithDifferentNumberOfChainsPerNode(
            systemSetup: SystemSetup
    ) {
        val peerList = systemSetup.toPeerInfoList()
        val peers = peerList.toTypedArray()
        configOverrides.setProperty("testpeerinfos", peers)

        // Creating node with two chains
        logger.debug("---Creating node with custom number of chains ----------------------------")

        val testName: String = this::class.java.simpleName ?: "NoName"   // Get subclass name or dummy
        for (nodeSetup in systemSetup.nodeMap.values) {
            val nodeConfigProvider = createNodeConfig( testName, nodeSetup, systemSetup)
            val newPTNode = PostchainTestNode(nodeConfigProvider, true)

            // TODO: not nice to mutate the "nodes" object like this, should return the list of PTNodes instead for testability
            nodes.add(newPTNode)
            nodeMap[nodeSetup.sequenceNumber] = newPTNode
        }


        // Asserting all chains are started
        // (This is a bit more complicated since we have different chains per node)
        logger.debug("---Asserting all chains are started -------------------------")
        Awaitility.await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    systemSetup.nodeMap.values.forEach { nodeSetup ->
                        val bcList = systemSetup.getBlockchainsANodeShouldRun(nodeSetup.sequenceNumber)
                        val node = nodeMap[nodeSetup.sequenceNumber]!!
                        bcList.forEach{ bcSetup -> node.assertChainStarted(bcSetup.chainId.toLong()) }
                    }
                }

        if (nodes.size > 1) {
            logger.debug("---Asserting all chains are connected -------------------------")
            // We don't need to assert all connections, just check some random connections
            Awaitility.await().atMost(Duration.TEN_SECONDS)
                    .untilAsserted {
                        systemSetup.nodeMap.values.forEach { nodeSetup ->

                            val nn =nodeSetup.sequenceNumber.nodeNumber
                            logger.debug("Assert connection for node: $nn")
                            /*
                            val nodesForBc = mutableListOf<PostchainTestNode>()
                            for (nodeSeqNr in bcSetup.signerNodeList) {
                                nodesForBc.add(nodeMap[nodeSeqNr]!!)
                            }
                            */

                            nodeSetup.getAllBlockchains().forEach { chainId ->
                                val allSignersButMe = systemSetup.blockchainMap[chainId]!!.signerNodeList.filter { it != nodeSetup.sequenceNumber }
                                if (logger.isDebugEnabled) {
                                    val debugOut = allSignersButMe.map { it.nodeNumber.toString() }.fold(",", String::plus)
                                    logger.debug("Wait for (node $nn) to be connected to all nodes in chain $chainId (nodes $debugOut)")
                                }
                                val allPostchainTesNodesButMe = allSignersButMe.map { nodeMap[it]!! }.toTypedArray()
                                nodeMap[nodeSetup.sequenceNumber]!!.assertNodeConnectedWith(chainId.toLong(), *allPostchainTesNodesButMe)
                            }
                        }
                    }
        }
    }




    /**
     * Note that we are enqueueing real GTX TXs here.
     *
     * @param blocksCount number of blocks to buildFromFile
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
                        buildBlocks(nodeId, chain, block) // The block we can buildFromFile ourselves
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
                chains.forEach {chain ->
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
