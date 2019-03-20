package net.postchain.integrationtest.reconnection

import assertk.assertions.isNotNull
import assertk.assertions.isNull
import net.postchain.containsExactlyKeys
import net.postchain.devtools.PostchainTestNode
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Assert
import org.junit.Test

class FourPeersReconnectionTest : ReconnectionTest() {

    @Test
    fun test4Peers() {
        val nodesCount = 4
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val blockchainConfig = "/net/postchain/stability/blockchain_config_4.xml"
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/stability/node0.properties",
                "classpath:/net/postchain/stability/node1.properties",
                "classpath:/net/postchain/stability/node2.properties",
                "classpath:/net/postchain/stability/node3.properties"
        )

        // Creating all peers
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig)
        }

        // Asserting that chain is started
        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()
                    assertk.assert(nodes[1].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()
                    assertk.assert(nodes[2].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()
                    assertk.assert(nodes[3].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()
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
        Awaitility.await().atMost(Duration.FIVE_SECONDS)
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
        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    Assert.assertEquals(1, queries(nodes[0]) { it.getBestHeight() })
                    Assert.assertEquals(1, queries(nodes[1]) { it.getBestHeight() })
                    Assert.assertEquals(1, queries(nodes[2]) { it.getBestHeight() })
                    Assert.assertEquals(1, queries(nodes[3]) { it.getBestHeight() })
                }

        // Shutting down node 3
        nodes[3].shutdown()

        // Asserting that
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    // chain is active for peer 0, 1, 2 and is shutdown for peer 3
                    assertk.assert(nodes[0].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()
                    assertk.assert(nodes[1].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()
                    assertk.assert(nodes[2].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()
                    assertk.assert(nodes[3].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNull()

                    // network topology is that peer 3 is disconnected from interconnected peers 0, 1, 2
                    assertk.assert(nodes[0].networkTopology()).containsExactlyKeys(
                            nodes[1].pubKey(), nodes[2].pubKey())
                    assertk.assert(nodes[1].networkTopology()).containsExactlyKeys(
                            nodes[0].pubKey(), nodes[2].pubKey())
                    assertk.assert(nodes[2].networkTopology()).containsExactlyKeys(
                            nodes[1].pubKey(), nodes[0].pubKey())
                    //assertk.assert(nodes[3].networkTopology()).isEmpty() // No assertion because chain already disconnected
                }

        // Removing peer 3
        nodes.removeAt(3)

        println("Re-boring peer 3")
        // Re-boring peer 1
        createSingleNode(3, nodesCount, nodeConfigsFilenames[1], blockchainConfig)

        // Asserting that
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    // chain is active for peer 3
                    assertk.assert(nodes[3].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()

                    // network topology is that peers 0, 1, 2, 3 are interconnected
                    assertk.assert(nodes[0].networkTopology()).containsExactlyKeys(
                            nodes[1].pubKey(), nodes[2].pubKey(), nodes[3].pubKey())
                    assertk.assert(nodes[1].networkTopology()).containsExactlyKeys(
                            nodes[0].pubKey(), nodes[2].pubKey(), nodes[3].pubKey())
                    assertk.assert(nodes[2].networkTopology()).containsExactlyKeys(
                            nodes[1].pubKey(), nodes[0].pubKey(), nodes[3].pubKey())
                    assertk.assert(nodes[3].networkTopology()).containsExactlyKeys(
                            nodes[1].pubKey(), nodes[2].pubKey(), nodes[0].pubKey())
                }

        // Asserting that height is
        Assert.assertEquals(1, queries(nodes[0]) { it.getBestHeight() })
        Assert.assertEquals(1, queries(nodes[1]) { it.getBestHeight() })
        Assert.assertEquals(1, queries(nodes[2]) { it.getBestHeight() })
        Assert.assertEquals(-1, queries(nodes[3]) { it.getBestHeight() })

        // Building a block 2 via newly connected peer 3
        nodes[3].let {
            enqueueTransactions(it, tx100, tx101)
            awaitBuiltBlock(it, 2)
        }
        // * Asserting height is 2 for all peers
        Awaitility.await().atMost(Duration.FIVE_SECONDS)
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