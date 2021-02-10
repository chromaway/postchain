package net.postchain.integrationtest.sync

import net.postchain.devtools.KeyPairHelper
import net.postchain.devtools.currentHeight
import net.postchain.network.x.XPeerID
import org.junit.Assert
import org.junit.Test
import java.lang.Thread.sleep
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForkTest : ManagedModeTest() {

    /*
    Tests

    Dimensions:
    Signer / Replica
    Height


     */

    @Test
    fun testSyncManagedBlockchain() {
        basicSystem()

        val c2 = startNewBlockchain(setOf(1), setOf(2))
        buildBlock(c2, 0)

        restartNodeClean(2, c2, -1)
        awaitHeight(c2, 0)
    }

    // Local sync

    @Test
    fun testForkLocallySimple() {
        val (c1, c2) = makeFork()
        assertEqualAtHeight(c1, c2, 10)
        // Can't build block until configuration without historic brid deployed
        assertCantBuildBlock(c2, 11)
    }

    @Test
    fun testForkLocallyAddConfCantBuild() {
        val (c1, c2) = makeFork()

        val c2_11 = addBlockchainConfiguration(c2, 11, setOf(0), setOf(1), c1.chain)
        buildBlock(c1, 11)
        awaitHeight(c2_11, 11)
        assertEqualAtHeight(c1, c2_11, 11)
        // Can't build block until configuration without historic brid deployed
        assertCantBuildBlock(c2, 12)
    }

    @Test
    fun testForkLocallyAddConfSameSignerCanBuild() {
        val (c1, c2) = makeFork()
        // c1 and c2 are in sync at height 10.

        val c2_12 = addBlockchainConfiguration(c2, 12, setOf(0), setOf(1), null)
        buildBlock(c1, 12)
        // Unfortunately, we must wait a full cycle of "synclocally (quick), fetch (2s), cross-fetch (2s)"
        // to be sure we don't sync from c1 after height 11. This sucks.
        sleep(5000)
        // Assert that c2_12 isn't syncing from c1 after height 11
        assertEquals(11, c2_12.nodes()[0].currentHeight(c2_12.chain))
        // Assert that c2_12 can build blocks on its own now
        buildBlock(c2_12, 12)
        assertNotEqualAtHeight(c1, c2_12, 12)
    }


    @Test
    fun testForkLocallySwapSignerReplica() {
        val c1 = basicSystem()
        val c2 = startNewBlockchain(setOf(1), setOf(0), c1.chain)
        sleep(1000)
        c2.nodes().forEach {
            assertEquals(-1, it.blockQueries(c2.chain).getBestHeight().get())
        }
    }

    // Network sync

    @Test
    fun testForkSignerCrossFetch() {
        startManagedSystem(2, 0)

        val c1 = startNewBlockchain(setOf(0), setOf(1))
        buildBlock(c1, 10)

        val expectedBlockRid = nodes[c1.all().first()].blockQueries(c1.chain).getBlockRid(10).get()

        // Make sure that node <node> doesn't have c1 locally and that the other node
        // doesn't have c2, We want node <node> to cross-fetch c2 from c1 on the other node instead of from local db.
        val nodeDataSource = dataSources(c1)[0]!!
        nodeDataSource.delBlockchain(chainRidOf(c1.chain))
        // Set node 1 as replica for c1 so that node 0 will use node 1 to cross-fetch blocks.
        nodeDataSource.extraReplicas.put(chainRidOf(1), listOf(XPeerID(KeyPairHelper.pubKey(1))))

        restartNodeClean(0, c0, -1)
        val c2 = startNewBlockchain(setOf(0), setOf(), c1.chain)
        awaitHeight(c2, 10)
        c2.all().forEach {
            Assert.assertArrayEquals(nodes[it].blockQueries(c2.chain).getBlockRid(10).get(), expectedBlockRid)
        }
        assertCantBuildBlock(c2, 11)
    }

    @Test
    fun testReplicaFetch() {
        startManagedSystem(2, 0)

        val c1 = startNewBlockchain(setOf(0), setOf(1))
        buildBlock(c1, 10)

        val expectedBlockRid = nodes[c1.all().first()].blockQueries(c1.chain).getBlockRid(10).get()

        // Make sure that node <node> doesn't have c1 locally and that the other node
        // doesn't have c2, We want node <node> to cross-fetch c2 from c1 on the other node instead of from local db.
        val nodeDataSource = dataSources(c1)[1]!!
        nodeDataSource.delBlockchain(chainRidOf(c1.chain))

        restartNodeClean(1, c0, -1)
        // We don't know if node 1 will fetch c1 or c2 from node 0, because
        // node 0 might sync parts of c2 locally from c1 before node 1 fetches
        // from node0. Se we can't be sure that both fetch and cross-fetch works for a replica,
        // only that either works.
        // If you can come up with a test that forces a replica to cross-fetch, it'd be great.
        val c2 = startNewBlockchain(setOf(0), setOf(1), c1.chain)
        awaitHeight(c2, 10)
        c2.all().forEach {
            Assert.assertArrayEquals(nodes[it].blockQueries(c2.chain).getBlockRid(10).get(), expectedBlockRid)
        }
        assertCantBuildBlock(c2, 11)
    }

    @Test
    fun testRecursiveFork() {
        val (c1, c2) = makeFork()
        val c2_15 = addBlockchainConfiguration(c2, 15, setOf(0), setOf(1), c1.chain)
        buildBlock(c1, 15)
        awaitChainRestarted(c2_15, 14)
        awaitHeight(c2_15, 15)
        val c3 = startNewBlockchain(setOf(0), setOf(), c1.chain)
        buildBlock(c1, 17)
        awaitHeight(c2_15, 17)
        awaitHeight(c3, 17)
        val c2_19 = addBlockchainConfiguration(c2_15, 19, setOf(2), setOf(1), null)
        val c3_19 = addBlockchainConfiguration(c3, 19, setOf(2), setOf(0), c2_19.chain)
        buildBlock(c1, 20)
        awaitChainRestarted(c2_19, 18)
        awaitChainRestarted(c3_19, 18)
        buildBlock(c2_19, 19)
        awaitHeight(c3_19, 19)
        assertEqualAtHeight(c2_19, c3_19, 19)
        assertNotEqualAtHeight(c1, c3_19, 19)
        val c3_21 = addBlockchainConfiguration(c3_19, 21, setOf(0), setOf(), null)
        buildBlock(c2_19, 22)
        awaitChainRestarted(c3_21, 20)
        buildBlock(c3_21, 22)
        assertNotEqualAtHeight(c2_19, c3_21, 21)
    }

    @Test
    fun testForkReplicaFromNetInvalidSignerSet() {
        val c1 = basicSystem()
        val c2 = startNewBlockchain(setOf(1), setOf(2), c1.chain)
        sleep(1000)
        c2.nodes().forEach {
            assertEquals(-1, it.blockQueries(c2.chain).getBestHeight().get())
        }
    }

    private fun awaitChainRestarted(nodeSet: NodeSet, atLeastHeight: Long) {
        nodeSet.all().forEach { awaitChainRunning(it, nodeSet.chain, atLeastHeight) }
    }

    private fun makeFork(): Pair<NodeSet, NodeSet> {
        val c1 = basicSystem()
        val c2 = startNewBlockchain(setOf(0), setOf(1), c1.chain)
        awaitHeight(c2, 10)
        return Pair(c1, c2)
    }

    fun addBlockchainConfiguration(chain: NodeSet, atHeight: Long, signers: Set<Int>, replicas: Set<Int>,
                                   historicChain: Long? = null): NodeSet {
        val newChain = NodeSet(chain.chain, signers, replicas)
        newBlockchainConfiguration(newChain, historicChain, atHeight)
        return newChain
    }

    fun assertEqualAtHeight(chainOld: NodeSet, chainNew: NodeSet, height: Long) {
        val expectedBlockRid = nodes[chainOld.all().first()].blockQueries(chainOld.chain).getBlockRid(height).get()
        chainNew.all().forEach {
            Assert.assertArrayEquals(nodes[it].blockQueries(chainNew.chain).getBlockRid(height).get(), expectedBlockRid)
        }
    }


    fun assertNotEqualAtHeight(chainOld: NodeSet, chainNew: NodeSet, height: Long) {
        val expectedBlockRid = nodes[chainOld.all().first()].blockQueries(chainOld.chain).getBlockRid(height).get()
        chainNew.all().forEach {
            Assert.assertFalse(expectedBlockRid!!.contentEquals(nodes[it].blockQueries(chainNew.chain).getBlockRid(height).get()!!))
        }
    }

    private var chainId: Long = 1
    fun startNewBlockchain(signers: Set<Int>, replicas: Set<Int>, historicChain: Long? = null): NodeSet {
        assertTrue(signers.intersect(replicas).isEmpty())
        val maxIndex = c0.all().size
        signers.forEach { assertTrue(it < maxIndex ) }
        replicas.forEach { assertTrue(it < maxIndex) }
        val c = NodeSet(chainId++, signers, replicas)
        newBlockchainConfiguration(c, historicChain)
        // Await blockchain started on all relevant nodes
        awaitChainRestarted(c, -1)
        return c
    }

    /**
     * Starts a managed system with one managed blockchain with 11 blocks:
     *
     * * chain0 has two signers (indices 0 and 1) and one replica (2)
     * * chain1 has one signer (0) and one replica (1)
     */
    private fun basicSystem(): NodeSet {
        startManagedSystem(2, 1)

        val c1 = startNewBlockchain(setOf(0), setOf(1))
        buildBlock(c1, 10)
        return c1
    }
}