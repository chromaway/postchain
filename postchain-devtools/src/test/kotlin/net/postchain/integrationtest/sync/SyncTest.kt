package net.postchain.integrationtest.sync

import mu.KLogging
import net.postchain.devtools.awaitHeight
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SyncTest(val signerCount: Int, val replicaCount: Int, val syncIndex: Set<Int>, val stopIndex: Set<Int>, val blocksToSync: Int) : AbstractSyncTest() {

    private companion object: KLogging() {

        @JvmStatic
        @Parameterized.Parameters
        fun testArguments() = listOf(
                // Single block test
                arrayOf(1, 1, setOf(0), setOf<Int>(), 1),
                arrayOf(1, 1, setOf(1), setOf<Int>(), 1),
                arrayOf(1, 2, setOf(1), setOf<Int>(0), 1),
                arrayOf(2, 0, setOf(1), setOf<Int>(), 1),

                // Multi block test
                arrayOf(1, 1, setOf(0), setOf<Int>(), 50),
                arrayOf(1, 1, setOf(1), setOf<Int>(), 50),
                arrayOf(1, 2, setOf(1), setOf<Int>(0), 50),
                arrayOf(2, 0, setOf(1), setOf<Int>(), 50),

                // Multi node multi blocks
                arrayOf(4, 4, setOf(0, 1, 2, 4, 5), setOf<Int>(3, 6), 50)
        )
    }

    /*
    The problem seems to be the same as in other p2p systems: How to find initial peers?

    Problems to solve:

    When a node (old or new) comes online it might not become aware of any currently
    active nodes because its nodelist might be outdated (network has evolved over time).

    It doesn’t have to happen at blockheight -1. This can happen at any height
    when the node has been offline for a while. This can only happen on chain0,
    because that's where all blockchain configurations and nodes are being tracked.
    As long as chain0 is fully synced, a node should be able to sync any other blockchain
    in that domain, because it has the most recent signerlist.

    Non-obvious tests to implement:


    1 Sync a managed blockchain (not chain0) from newest nodes, where none of the initial nodes exist. (Should work fine)
    2 Sync chain0 from newest nodes, where none of the initial nodes exist. Should work because newest nodes know about me.
    3 Test 2, but give it a syncnode via configuration (Should work)
    4 Sync single sole node from single nodelist (should wait for discovery timeout)

     */

    private fun n(index: Int): String {
        var p = nodes[index].pubKey
        return p.substring(0, 4) + ":" + p.substring(64)
    }

    @Test
    fun sync() {
        val nodeSetups = runNodes(signerCount, replicaCount)
        logger.debug { "All nodes started" }
        buildBlock(0, blocksToSync-1L)
        logger.debug { "All nodes have block ${blocksToSync-1}" }

        val expectedBlockRid = nodes[0].blockQueries(0).getBlockRid(blocksToSync-1L).get()

        val peerInfos = nodeSetups[0].configurationProvider!!.getConfiguration().peerInfoMap
        stopIndex.forEach {
            logger.debug { "Shutting down ${n(it)}" }
            nodes[it].shutdown()
            logger.debug { "Shutting down ${n(it)} done" }
        }
        syncIndex.forEach {
            logger.debug { "Restarting clean ${n(it)}" }
            restartNodeClean(nodeSetups[it])
            logger.debug { "Restarting clean ${n(it)} done" }
        }

        syncIndex.forEach {
            logger.debug { "Awaiting height 0 on ${n(it)}" }
            nodes[it].awaitHeight(0, blocksToSync-1L)
            val actualBlockRid = nodes[it].blockQueries(0).getBlockRid(blocksToSync-1L).get()
            assertArrayEquals(expectedBlockRid, actualBlockRid)
            logger.debug { "Awaiting height 0 on ${n(it)} done" }
        }

        stopIndex.forEach {
            logger.debug { "Start ${n(it)} again" }
            startOldNode(it, peerInfos, nodeSetups[it])
        }
        buildBlock(0, blocksToSync.toLong())
        logger.debug { "All nodes has block $blocksToSync" }
    }

}