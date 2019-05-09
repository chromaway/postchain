package net.postchain.integrationtest.reconnection

import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_ID
import net.postchain.integrationtest.assertChainNotStarted
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.assertNodeConnectedWith
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Assert
import org.junit.Test

class FourPeersReconnectionTest : ReconnectionTest() {

    @Test
    fun test4Peers() {
        val nodesCount = 4
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val blockchainConfig = "/net/postchain/reconnection/blockchain_config_4.xml"
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/reconnection/node0.properties",
                "classpath:/net/postchain/reconnection/node1.properties",
                "classpath:/net/postchain/reconnection/node2.properties",
                "classpath:/net/postchain/reconnection/node3.properties"
        )

        // Creating all peers
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig)
        }

        // Asserting that chain is started
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    nodes.forEach { it.assertChainStarted() }
                }

        // Asserting height is -1 for all peers
        Assert.assertEquals(-1, queries(nodes[0]) { it.getBestHeight() })
        Assert.assertEquals(-1, queries(nodes[1]) { it.getBestHeight() })
        Assert.assertEquals(-1, queries(nodes[2]) { it.getBestHeight() })
        Assert.assertEquals(-1, queries(nodes[3]) { it.getBestHeight() })

        // Building a block 0 via peer 0
        nodes[0].let {
            enqueueTransactions(it, tx0, tx1)
            awaitBuiltBlock(it, 0)
        }
        // * Asserting height is 0 for all peers
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    Assert.assertEquals(0, queries(nodes[0]) { it.getBestHeight() })
                    Assert.assertEquals(0, queries(nodes[1]) { it.getBestHeight() })
                    Assert.assertEquals(0, queries(nodes[2]) { it.getBestHeight() })
                    Assert.assertEquals(0, queries(nodes[3]) { it.getBestHeight() })
                }

        // Again: Building a block 1 via peer 1
        nodes[1].let {
            enqueueTransactions(it, tx10, tx11)
            awaitBuiltBlock(it, 1)
        }
        // * Asserting height is 1 for all peers
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    Assert.assertEquals(1, queries(nodes[0]) { it.getBestHeight() })
                    Assert.assertEquals(1, queries(nodes[1]) { it.getBestHeight() })
                    Assert.assertEquals(1, queries(nodes[2]) { it.getBestHeight() })
                    Assert.assertEquals(1, queries(nodes[3]) { it.getBestHeight() })
                }

        // Shutting down node 3
        nodes[3].shutdown()

        // Asserting that
        await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    // chain is active for peer 0, 1, 2 and is shutdown for peer 3
                    nodes[0].assertChainStarted()
                    nodes[1].assertChainStarted()
                    nodes[2].assertChainStarted()
                    nodes[3].assertChainNotStarted()

                    // network topology is that peer 3 is disconnected from interconnected peers 0, 1, 2
                    nodes[0].assertNodeConnectedWith(DEFAULT_CHAIN_ID, nodes[1], nodes[2])
                    nodes[1].assertNodeConnectedWith(DEFAULT_CHAIN_ID, nodes[0], nodes[2])
                    nodes[2].assertNodeConnectedWith(DEFAULT_CHAIN_ID, nodes[1], nodes[0])
//                    nodes[3].assertNodeConnectedWith(...) // No assertion because chain already disconnected
                }

        // Removing peer 3
        nodes.removeAt(3)

        println("Re-boring peer 3")
        // Re-boring peer 1
        createSingleNode(3, nodesCount, nodeConfigsFilenames[1], blockchainConfig)

        // Asserting that
        await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    // chain is active for peer 3
                    nodes[3].assertChainStarted()

                    // network topology is that peers 0, 1, 2, 3 are interconnected
                    nodes[0].assertNodeConnectedWith(DEFAULT_CHAIN_ID, nodes[1], nodes[2], nodes[3])
                    nodes[1].assertNodeConnectedWith(DEFAULT_CHAIN_ID, nodes[0], nodes[2], nodes[3])
                    nodes[2].assertNodeConnectedWith(DEFAULT_CHAIN_ID, nodes[1], nodes[0], nodes[3])
                    nodes[3].assertNodeConnectedWith(DEFAULT_CHAIN_ID, nodes[1], nodes[2], nodes[0])
                }

        /* It's not correct to assert that height is -1 for peer 3
        Assert.assertEquals(-1, queries(nodes[3]) { it.getBestHeight() })
        */

        // Building a block 2 via newly connected peer 3
        nodes[3].let {
            enqueueTransactions(it, tx100, tx101)
            awaitBuiltBlock(it, 2)
        }
        // * Asserting height is 2 for all peers
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    Assert.assertEquals(2, queries(nodes[0]) { it.getBestHeight() })
                    Assert.assertEquals(2, queries(nodes[1]) { it.getBestHeight() })
                    Assert.assertEquals(2, queries(nodes[2]) { it.getBestHeight() })
                    Assert.assertEquals(2, queries(nodes[3]) { it.getBestHeight() })
                }

        // Asserts txs in blocks
        (0..3).forEach { i ->
            assertThatNodeInBlockHasTxs(nodes[i], 0, tx0, tx1)
            assertThatNodeInBlockHasTxs(nodes[i], 1, tx10, tx11)
            assertThatNodeInBlockHasTxs(nodes[i], 2, tx100, tx101)
        }
    }
}