package net.postchain.util

import mu.KLogging
import net.postchain.common.toHex
import net.postchain.configurations.GTXTestModule
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.TxCache
import net.postchain.devtools.testinfra.TestOneOpGtxTransaction
import net.postchain.devtools.utils.configuration.NodeSeqNumber
import net.postchain.devtools.utils.configuration.SystemSetup
import net.postchain.gtx.GTXTransactionFactory
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.assertNodeConnectedWith
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Assert
import kotlin.test.assertNotNull

/**
 * Extends [IntegrationTestSetup] with extra functions relevant for real GTX transactions on multi chain tests
 *
 * We are using (real) [GTXTransaction] here, not some mock transaction as otherwise is common for integration tests.
 */
open class GtxTxIntegrationTestSetup: IntegrationTestSetup()  {

    private val gtxTestModule =  GTXTestModule()
    private val factoryMap: MutableMap<Long, GTXTransactionFactory> = mutableMapOf()

    companion object: KLogging()


    private fun strategyOf(nodeId: Int, chainId: Long): OnDemandBlockBuildingStrategy {
        return nodes[nodeId].blockBuildingStrategy(chainId) as OnDemandBlockBuildingStrategy
    }


    /**
     * Starts the nodes according to the settings found in [SystemSetup]
     *
     * 1. First we create the [PostchainTestNode] s from the [SystemSetup]
     * 2. Then we make sure the chains have started
     * 3. Then we make sure chains are connected
     *
     * @param systemSetup holds the detailed description of the system
     */
    fun runXNodes(
            systemSetup: SystemSetup
    ) {
        logger.debug("---1. Creating nodes ----------------------------")
        createNodesFromSystemSetup(systemSetup)

        // Asserting all chains are started
        // (This is a bit more complicated since we have different chains per node)
        logger.debug("---2. Asserting all chains are started -------------------------")
        Awaitility.await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    systemSetup.nodeMap.values.forEach { nodeSetup ->
                        val bcList = systemSetup.getBlockchainsANodeShouldRun(nodeSetup.sequenceNumber)
                        val node = nodeMap[nodeSetup.sequenceNumber]!!
                        bcList.forEach{ bcSetup -> node.assertChainStarted(bcSetup.chainId.toLong()) }
                    }
                }


        if (nodes.size > 1) {
            logger.debug("---3. Asserting all chains are connected -------------------------")
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
     * We are assuming that the relevant chains have been added and started on the nodes.
     *
     * @param blocksCount number of blocks to build
     * @param txPerBlock number of TX in each block
     * @param chainList all BC we will use
     * @param nodeNameWithBlockchainsArr = null if all nodes have the same chains, else with a value
     */
    fun runXNodesWithYTxPerBlock(
            blocksCount: Int,
            txPerBlock: Int,
            systemSetup: SystemSetup,
            txCache: TxCache
    ) {

        if(factoryMap.isEmpty()) {
            // Must create the TX factories before any transactions can be created
            systemSetup.blockchainMap.values.forEach { chainSetup ->
                factoryMap[chainSetup.chainId.toLong()] = GTXTransactionFactory(chainSetup.rid, gtxTestModule, cryptoSystem)
            }
        }

        // Enqueueing txs
        logger.debug("---Enqueueing txs --------------------------------------------")
        var txId = 0
        for (block in 0 until blocksCount) {
            for (blockIndex in 0 until txPerBlock) {
                val currentTxId = txId++
                logger.debug("+++++++++++++++++++++++++++++++++++++++++++")
                logger.debug("++++ block: $block, txId: $txId +++++++")
                logger.debug("+++++++++++++++++++++++++++++++++++++++++++")
                systemSetup.nodeMap.values.forEach { node ->
                    node.chainsToSign.forEach { chain -> // For each chain this node is a signer of
                        enqueueTx(chain.toLong(), currentTxId, txCache, block, blockIndex, nodes[node.sequenceNumber.nodeNumber])
                    }
                }
            }

            systemSetup.nodeMap.values.forEach { node ->
                val nodeId = node.sequenceNumber.nodeNumber
                node.chainsToSign.forEach { chain ->
                    buildBlocks(nodeId, chain.toLong(), block) // The block we can buildgetPeerInfoMap ourselves
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
            systemSetup: SystemSetup,
            txCache: TxCache
    ) {
        logger.debug("---Assertions -------------------------------------------------")

        val expectedHeight = (blocksCount - 1).toLong()
        //logger.debug("Nr of nodes: ${nodes.size}, total nr of blocks: $blocksCount => expected height: $expectedHeight")

        systemSetup.nodeMap.values.forEach { node ->
                    node.chainsToSign.forEach { chain ->
                        assertChainForNode(node.sequenceNumber, chain.toLong(), expectedHeight, txPerBlock, txCache)
                }
        }
    }

    private fun assertChainForNode(nodeId: NodeSeqNumber, chain: Long, expectedHeight: Long, txPerBlock: Int, txCache: TxCache) {
        logger.info { "Assertions: node: $nodeId, chain: $chain, expectedHeight: $expectedHeight" }
        val node = nodes[nodeId.nodeNumber]

        val queries = node.blockQueries(chain)

        // Asserting best height equals to expected
        val best = queries.getBestHeight().get()
        Assert.assertEquals(expectedHeight, best)

        for (height in 0..expectedHeight) {
            logger.info { "Verifying height $height" }

            // Asserting uniqueness of block at height
            val blockRids = queries.getBlockRid(height).get()
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
