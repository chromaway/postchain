// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.devtools.base

import net.postchain.core.*
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.testinfra.ErrorTransaction
import net.postchain.devtools.testinfra.TestBlockchainConfiguration
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.devtools.testinfra.UnexpectedExceptionTransaction
import org.junit.Assert.*
import org.junit.Test

class BlockchainEngineTest : IntegrationTest() {

    @Test
    fun testBuildBlock() {
        configOverrides.setProperty("infrastructure", "base/test")
        val node = createNode(0, "/net/postchain/blocks/blockchain_config.xml")
        val txQueue = node.getBlockchainInstance().getEngine().getTransactionQueue()

        txQueue.enqueue(TestTransaction(0))
        buildBlockAndCommit(node)
        assertEquals(0, getBestHeight(node))
        val riDsAtHeight0 = getTxRidsAtHeight(node, 0)
        assertEquals(1, riDsAtHeight0.size)
        assertArrayEquals(TestTransaction(id = 0).getRID(), riDsAtHeight0[0])

        txQueue.enqueue(TestTransaction(1))
        txQueue.enqueue(TestTransaction(2))
        buildBlockAndCommit(node)
        assertEquals(1, getBestHeight(node))
        assertTrue(riDsAtHeight0.contentDeepEquals(getTxRidsAtHeight(node, 0)))
        val riDsAtHeight1 = getTxRidsAtHeight(node, 1)
        assertTrue(riDsAtHeight1.contentDeepEquals(Array(2, { TestTransaction(it + 1).getRID() })))

        // Empty block. All tx but last (10) will be failing
        txQueue.enqueue(TestTransaction(3, good = true, correct = false))
        txQueue.enqueue(TestTransaction(4, good = false, correct = true))
        txQueue.enqueue(TestTransaction(5, good = false, correct = false))
        txQueue.enqueue(ErrorTransaction(6, true, true))
        txQueue.enqueue(ErrorTransaction(7, false, true))
        txQueue.enqueue(ErrorTransaction(8, true, false))
        txQueue.enqueue(UnexpectedExceptionTransaction(9))
        txQueue.enqueue(TestTransaction(10))

        buildBlockAndCommit(node)
        assertEquals(2, getBestHeight(node))
        assertTrue(riDsAtHeight1.contentDeepEquals(getTxRidsAtHeight(node, 1)))
        val txRIDsAtHeight2 = getTxRidsAtHeight(node, 2)
        assertEquals(1, txRIDsAtHeight2.size)
        assertArrayEquals(TestTransaction(10).getRID(), txRIDsAtHeight2[0])
    }

    @Test
    fun testLoadUnfinishedEmptyBlock() {
        val (node0, node1) = createNodes(2, "/net/postchain/blocks/blockchain_config_2.xml")

        val blockData = createBlockWithTxAndCommit(node0, 0)

        loadUnfinishedAndCommit(node1, blockData)
        assertEquals(0, getBestHeight(node1))
        val riDsAtHeight0 = getTxRidsAtHeight(node1, 0)
        assertEquals(0, riDsAtHeight0.size)
    }

    @Test
    fun testLoadUnfinishedBlock2tx() {
        val (node0, node1) = createNodes(2, "/net/postchain/blocks/blockchain_config_2.xml")

        val blockData = createBlockWithTxAndCommit(node0, 2)
        loadUnfinishedAndCommit(node1, blockData)

        assertEquals(0, getBestHeight(node1))
        val riDsAtHeight0 = getTxRidsAtHeight(node1, 0)
        assertTrue(riDsAtHeight0.contentDeepEquals(Array(2) { TestTransaction(it).getRID() }))
    }

    @Test
    fun testMultipleLoadUnfinishedBlocks() {
        val (node0, node1) = createNodes(2, "/net/postchain/blocks/blockchain_config_2.xml")

        for (i in 0..10) {
            val blockData = createBlockWithTxAndCommit(node0, 2, i * 2)
            loadUnfinishedAndCommit(node1, blockData)

            assertEquals(i.toLong(), getBestHeight(node1))
            val riDsAtHeighti = getTxRidsAtHeight(node1, i.toLong())
            assertTrue(riDsAtHeighti.contentDeepEquals(Array(2) { TestTransaction(i * 2 + it).getRID() }))
        }
    }

    @Test
    fun testLoadUnfinishedBlockTxFail() {
        val (node0, node1) = createNodes(2, "/net/postchain/blocks/blockchain_config_2.xml")

        val blockData = createBlockWithTxAndCommit(node0, 2)

        val bc = node1.getBlockchainInstance().getEngine().getConfiguration() as TestBlockchainConfiguration
        // Make the tx invalid on follower. Should discard whole block
        bc.transactionFactory.specialTxs[0] = ErrorTransaction(0, true, false)
        try {
            loadUnfinishedAndCommit(node1, blockData)
            fail()
        } catch (userMistake: UserMistake) {
            // Expected
        }
        // Block must not have been created.
        assertEquals(-1, getBestHeight(node1))

        bc.transactionFactory.specialTxs.clear()
        // And we can create a new valid block afterwards.
        loadUnfinishedAndCommit(node1, blockData)

        assertEquals(0, getBestHeight(node1))
        val riDsAtHeight0 = getTxRidsAtHeight(node1, 0)
        assertTrue(riDsAtHeight0.contentDeepEquals(Array(2) { TestTransaction(it).getRID() }))
    }

    @Test
    fun testLoadUnfinishedBlockInvalidHeader() {
        val (node0, node1) = createNodes(2, "/net/postchain/blocks/blockchain_config_2.xml")

        val blockData = createBlockWithTxAndCommit(node0, 2)
        blockData.header.prevBlockRID[0]++
        try {
            loadUnfinishedAndCommit(node1, blockData)
            fail()
        } catch (userMistake: UserMistake) {
            // Expected
        }
        // Block must not have been created.
        assertEquals(-1, getBestHeight(node1))
    }

    @Test
    fun testAddBlock() {
        val (node0, node1) = createNodes(2, "/net/postchain/blocks/blockchain_config_2.xml")

        val blockBuilder = createBlockWithTx(node0, 2)
        val witness = commitBlock(blockBuilder)
        val blockData = blockBuilder.getBlockData()
        val blockWithWitness = BlockDataWithWitness(blockData.header, blockData.transactions, witness)

        node1.getBlockchainInstance().getEngine().addBlock(blockWithWitness)

        assertEquals(0, getBestHeight(node1))
        val riDsAtHeight0 = getTxRidsAtHeight(node1, 0)
        assertTrue(riDsAtHeight0.contentDeepEquals(Array(2) { TestTransaction(it).getRID() }))
    }

    private fun createBlockWithTxAndCommit(node: PostchainTestNode, txCount: Int, startId: Int = 0): BlockData {
        val blockBuilder = createBlockWithTx(node, txCount, startId)
        commitBlock(blockBuilder)
        return blockBuilder.getBlockData()
    }

    private fun createBlockWithTx(node: PostchainTestNode, txCount: Int, startId: Int = 0): BlockBuilder {
        val engine = node.getBlockchainInstance().getEngine()
        (startId until startId + txCount).forEach {
            engine.getTransactionQueue().enqueue(TestTransaction(it))
        }
        return engine.buildBlock()
    }

    private fun loadUnfinishedAndCommit(node: PostchainTestNode, blockData: BlockData) {
        val blockBuilder = node.getBlockchainInstance().getEngine().loadUnfinishedBlock(blockData)
        commitBlock(blockBuilder)
    }

    private fun commitBlock(blockBuilder: BlockBuilder): BlockWitness {
        val witnessBuilder = blockBuilder.getBlockWitnessBuilder() as MultiSigBlockWitnessBuilder
        assertNotNull(witnessBuilder)
        val blockData = blockBuilder.getBlockData()
        // Simulate other peers sign the block
        val blockHeader = blockData.header
        var i = 0
        while (!witnessBuilder.isComplete()) {
            val sigMaker = cryptoSystem.buildSigMaker(pubKey(i), privKey(i))
            witnessBuilder.applySignature(sigMaker.signDigest(blockHeader.blockRID))
            i++
        }
        val witness = witnessBuilder.getWitness()
        blockBuilder.commit(witness)
        return witness
    }

}