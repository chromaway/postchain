// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.test.base

import net.postchain.core.*
import net.postchain.test.IntegrationTest
import net.postchain.test.PostchainTestNode
import org.junit.Assert.*
import org.junit.Test

class BlockchainEngineTest : IntegrationTest() {

    @Test
    fun testBuildBlock() {
        val (node, chainId) = createNode(0)
        val txQueue = node.getBlockchainInstance(chainId).getEngine().getTransactionQueue()

        txQueue.enqueue(TestTransaction(0))
        buildBlockAndCommit(node, chainId)
        assertEquals(0, getBestHeight(node, chainId))
        val riDsAtHeight0 = getTxRidsAtHeight(node, chainId, 0)
        assertEquals(1, riDsAtHeight0.size)
        assertArrayEquals(TestTransaction(id = 0).getRID(), riDsAtHeight0[0])

        txQueue.enqueue(TestTransaction(1))
        txQueue.enqueue(TestTransaction(2))
        buildBlockAndCommit(node, chainId)
        assertEquals(1, getBestHeight(node, chainId))
        assertTrue(riDsAtHeight0.contentDeepEquals(getTxRidsAtHeight(node, chainId, 0)))
        val riDsAtHeight1 = getTxRidsAtHeight(node, chainId, 1)
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

        buildBlockAndCommit(node, chainId)
        assertEquals(2, getBestHeight(node, chainId))
        assertTrue(riDsAtHeight1.contentDeepEquals(getTxRidsAtHeight(node, chainId, 1)))
        val txRIDsAtHeight2 = getTxRidsAtHeight(node, chainId, 2)
        assertEquals(1, txRIDsAtHeight2.size)
        assertArrayEquals(TestTransaction(10).getRID(), txRIDsAtHeight2[0])
    }

    @Test
    fun testLoadUnfinishedEmptyBlock() {
        // TODO: Refactor this
        val nodes = createNodes(2)
        val (node0, chainId0) = nodes[0]
        val (node1, chainId1) = nodes[1]

        val blockData = createBlockWithTxAndCommit(node0, chainId0, 0)

        loadUnfinishedAndCommit(node1, chainId1, blockData)
        assertEquals(0, getBestHeight(node1, chainId1))
        val riDsAtHeight0 = getTxRidsAtHeight(node1, chainId1, 0)
        assertEquals(0, riDsAtHeight0.size)
    }

    @Test
    fun testLoadUnfinishedBlock2tx() {
        // TODO: Refactor this
        val nodes = createNodes(2)
        val (node0, chainId0) = nodes[0]
        val (node1, chainId1) = nodes[1]

        val blockData = createBlockWithTxAndCommit(node0, chainId0, 2)
        loadUnfinishedAndCommit(node1, chainId1, blockData)

        assertEquals(0, getBestHeight(node1, chainId1))
        val riDsAtHeight0 = getTxRidsAtHeight(node1, chainId0, 0)
        assertTrue(riDsAtHeight0.contentDeepEquals(Array(2) { TestTransaction(it).getRID() }))
    }

    @Test
    fun testMultipleLoadUnfinishedBlocks() {
        // TODO: Refactor this
        val nodes = createNodes(2)
        val (node0, chainId0) = nodes[0]
        val (node1, chainId1) = nodes[1]

        for (i in 0..10) {
            val blockData = createBlockWithTxAndCommit(node0, chainId0, 2, i * 2)
            loadUnfinishedAndCommit(node1, chainId1, blockData)

            assertEquals(i.toLong(), getBestHeight(node1, chainId1))
            val riDsAtHeighti = getTxRidsAtHeight(node1, chainId1, i.toLong())
            assertTrue(riDsAtHeighti.contentDeepEquals(Array(2) { TestTransaction(i * 2 + it).getRID() }))
        }
    }

    @Test
    fun testLoadUnfinishedBlockTxFail() {
        // TODO: Refactor this
        val nodes = createNodes(2)
        val (node0, chainId0) = nodes[0]
        val (node1, chainId1) = nodes[1]

        val blockData = createBlockWithTxAndCommit(node0, chainId0, 2)

        val bc = node1.getBlockchainInstance(chainId0).blockchainConfiguration as TestBlockchainConfiguration
        // Make the tx invalid on follower. Should discard whole block
        bc.transactionFactory.specialTxs.put(0, ErrorTransaction(0, true, false))
        try {
            loadUnfinishedAndCommit(node1, chainId1, blockData)
            fail()
        } catch (userMistake: UserMistake) {
            // Expected
        }
        // Block must not have been created.
        assertEquals(-1, getBestHeight(node1, chainId1))

        bc.transactionFactory.specialTxs.clear()
        // And we can create a new valid block afterwards.
        loadUnfinishedAndCommit(node1, chainId1, blockData)

        assertEquals(0, getBestHeight(node1, chainId1))
        val riDsAtHeight0 = getTxRidsAtHeight(node1, chainId1, 0)
        assertTrue(riDsAtHeight0.contentDeepEquals(Array(2) { TestTransaction(it).getRID() }))
    }

    @Test
    fun testLoadUnfinishedBlockInvalidHeader() {
        // TODO: Refactor this
        val nodes = createNodes(2)
        val (node0, chainId0) = nodes[0]
        val (node1, chainId1) = nodes[1]

        val blockData = createBlockWithTxAndCommit(node0, chainId0, 2)
        blockData.header.prevBlockRID[0]++
        try {
            loadUnfinishedAndCommit(node1, chainId1, blockData)
            fail()
        } catch (userMistake: UserMistake) {
            // Expected
        }
        // Block must not have been created.
        assertEquals(-1, getBestHeight(node1, chainId1))
    }

    @Test
    fun testAddBlock() {
        // TODO: Refactor this
        val nodes = createNodes(2)
        val (node0, chainId0) = nodes[0]
        val (node1, chainId1) = nodes[1]

        val blockBuilder = createBlockWithTx(node0, chainId0, 2)
        val witness = commitBlock(blockBuilder)
        val blockData = blockBuilder.getBlockData()
        val blockWithWitness = BlockDataWithWitness(blockData.header, blockData.transactions, witness)

        node1.getBlockchainInstance(1).getEngine().addBlock(blockWithWitness)

        assertEquals(0, getBestHeight(node1, chainId1))
        val riDsAtHeight0 = getTxRidsAtHeight(node1, chainId1, 0)
        assertTrue(riDsAtHeight0.contentDeepEquals(Array(2) { TestTransaction(it).getRID() }))
    }

    private fun createBlockWithTxAndCommit(node: PostchainTestNode, chainId: Long, txCount: Int, startId: Int = 0): BlockData {
        val blockBuilder = createBlockWithTx(node, chainId, txCount, startId)
        commitBlock(blockBuilder)
        return blockBuilder.getBlockData()
    }

    private fun createBlockWithTx(node: PostchainTestNode, chainId: Long, txCount: Int, startId: Int = 0): BlockBuilder {
        val engine = node.getBlockchainInstance(chainId).getEngine()
        (startId until startId + txCount).forEach {
            engine.getTransactionQueue().enqueue(TestTransaction(it))
        }
        return engine.buildBlock()
    }

    private fun loadUnfinishedAndCommit(node: PostchainTestNode, chainId: Long, blockData: BlockData) {
        val blockBuilder = node.getBlockchainInstance(chainId).getEngine().loadUnfinishedBlock(blockData)
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
            witnessBuilder.applySignature(cryptoSystem.makeSigner(pubKey(i), privKey(i))(blockHeader.rawData))
            i++
        }
        val witness = witnessBuilder.getWitness()
        blockBuilder.commit(witness)
        return witness
    }

}