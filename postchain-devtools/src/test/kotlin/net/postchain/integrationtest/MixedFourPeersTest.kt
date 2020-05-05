// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest

import net.postchain.devtools.ConfigFileBasedIntegrationTest
import net.postchain.devtools.testinfra.TestTransaction
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

/**
 * Mixed Four Peers Test is s mix of Reconnection, Reconfiguration and Multiple Chains tests.
 * See [FourPeersReconnectionTest], [FourPeersReconfigurationTest], [FourPeersMultipleChainsOperationsTest].
 */
@Ignore
class MixedFourPeersTest : ConfigFileBasedIntegrationTest() {

    private val tx1_0 = TestTransaction(10)
    private val tx1_1 = TestTransaction(11)
    private val tx1_2 = TestTransaction(12)
    private val tx1_3 = TestTransaction(13)

    private val tx2_0 = TestTransaction(20)
    private val tx2_1 = TestTransaction(21)
    private val tx2_2 = TestTransaction(22)
    private val tx2_3 = TestTransaction(23)

    @Test
    fun mixedTest() {
        val nodesCount = 4
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/mixed_test/four_peers/node0.properties",
                "classpath:/net/postchain/mixed_test/four_peers/node1.properties",
                "classpath:/net/postchain/mixed_test/four_peers/node2.properties",
                "classpath:/net/postchain/mixed_test/four_peers/node3.properties"
        )

        // chain 1 (peers 0, 1, 2, 3)
        val chainId1 = 1L
        val blockchainConfig1_1 = readBlockchainConfig(
                "/net/postchain/devtools/mixed_test/four_peers/blockchain_config_1_1.xml")
        val blockchainConfig1_2 = readBlockchainConfig(
                "/net/postchain/devtools/mixed_test/four_peers/blockchain_config_1_2.xml")

        // chain 2 (peers 0, 1, 2)
        val chainId2 = 2L
        val blockchainConfig2_1 = readBlockchainConfig(
                "/net/postchain/devtools/mixed_test/four_peers/blockchain_config_2_1.xml")
        val blockchainConfig2_2 = readBlockchainConfig(
                "/net/postchain/devtools/mixed_test/four_peers/blockchain_config_2_2.xml")

        // Creating all peers w/o chains
        createMultipleChainNodes(nodesCount, nodeConfigsFilenames, arrayOf())

        // Asserting that
        await().pollDelay(Duration.FIVE_SECONDS)
                .atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 1 is not started for all peers
                    nodes[0].assertChainNotStarted(chainId1)
                    nodes[1].assertChainNotStarted(chainId1)
                    nodes[2].assertChainNotStarted(chainId1)
                    nodes[3].assertChainNotStarted(chainId1)

                    // chain 2 is not started for all peers
                    nodes[0].assertChainNotStarted(chainId2)
                    nodes[1].assertChainNotStarted(chainId2)
                    nodes[2].assertChainNotStarted(chainId2)

                    // He we can't assert any network topology
                }

        // Launching chain 1 at peers 0, 1, 2, 3
        nodes[0].addBlockchainAndStart(chainId1, blockchainConfig1_1)
        nodes[1].addBlockchainAndStart(chainId1, blockchainConfig1_1)
        nodes[2].addBlockchainAndStart(chainId1, blockchainConfig1_1)
        nodes[3].addBlockchainAndStart(chainId1, blockchainConfig1_1)
        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 1 is started at peers 0, 1, 2, 3
                    nodes[0].assertChainStarted(chainId1)
                    nodes[1].assertChainStarted(chainId1)
                    nodes[2].assertChainStarted(chainId1)
                    nodes[3].assertChainStarted(chainId1)

                    // network topology of chain 1 is that peers 0, 1, 3, 4 are interconnected
                    nodes[0].assertNodeConnectedWith(chainId1, nodes[1], nodes[2], nodes[3])
                    nodes[1].assertNodeConnectedWith(chainId1, nodes[0], nodes[2], nodes[3])
                    nodes[2].assertNodeConnectedWith(chainId1, nodes[1], nodes[0], nodes[3])
                    nodes[3].assertNodeConnectedWith(chainId1, nodes[1], nodes[2], nodes[0])
                }

        // Launching chain 2 at peers 0, 1, 2
        nodes[0].addBlockchainAndStart(chainId2, blockchainConfig2_1)
        nodes[1].addBlockchainAndStart(chainId2, blockchainConfig2_1)
        nodes[2].addBlockchainAndStart(chainId2, blockchainConfig2_1)
        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 2 is started at peers 0, 1, 2
                    nodes[0].assertChainStarted(chainId2)
                    nodes[1].assertChainStarted(chainId2)
                    nodes[2].assertChainStarted(chainId2)

                    // network topology of chain 2 is that peers 0, 1, 2 are interconnected
                    nodes[0].assertNodeConnectedWith(chainId2, nodes[1], nodes[2])
                    nodes[1].assertNodeConnectedWith(chainId2, nodes[0], nodes[2])
                    nodes[2].assertNodeConnectedWith(chainId2, nodes[1], nodes[0])
                }

        // Adding blockchainConfig1_2 of chain1 with DummyModule2 at height 2 to all peers
        nodes[0].addConfiguration(chainId1, 2, blockchainConfig1_2)
        nodes[1].addConfiguration(chainId1, 2, blockchainConfig1_2)
        nodes[2].addConfiguration(chainId1, 2, blockchainConfig1_2)
        nodes[3].addConfiguration(chainId1, 2, blockchainConfig1_2)

        // Adding blockchainConfig2_2 of chain2 with DummyModule2 at height 3 to all peers
        nodes[0].addConfiguration(chainId2, 3, blockchainConfig2_2)
        nodes[1].addConfiguration(chainId2, 3, blockchainConfig2_2)
        nodes[2].addConfiguration(chainId2, 3, blockchainConfig2_2)

        // Asserting height is -1 for all peers for both chains
        // * chain 1
        assertEquals(-1L, nodes[0].query(chainId1) { it.getBestHeight() })
        assertEquals(-1L, nodes[1].query(chainId1) { it.getBestHeight() })
        assertEquals(-1L, nodes[2].query(chainId1) { it.getBestHeight() })
        assertEquals(-1L, nodes[3].query(chainId1) { it.getBestHeight() })
        // * chain 2
        assertEquals(-1L, nodes[0].query(chainId2) { it.getBestHeight() })
        assertEquals(-1L, nodes[1].query(chainId2) { it.getBestHeight() })
        assertEquals(-1L, nodes[2].query(chainId2) { it.getBestHeight() })

        // Building a block 0 at chain 1 via peer 0
        nodes[0].enqueueTxsAndAwaitBuiltBlock(chainId1, 0, tx1_0)
        // * Asserting height is 0 for all peers
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    assertEquals(0L, nodes[0].query(chainId1) { it.getBestHeight() })
                    assertEquals(0L, nodes[1].query(chainId1) { it.getBestHeight() })
                    assertEquals(0L, nodes[2].query(chainId1) { it.getBestHeight() })
                    assertEquals(0L, nodes[3].query(chainId1) { it.getBestHeight() })
                }

        // Building a block 0 at chain 2 via peer 1
        nodes[1].enqueueTxsAndAwaitBuiltBlock(chainId2, 0, tx2_0)
        // * Asserting height is 0 for all peers
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    assertEquals(0L, nodes[0].query(chainId2) { it.getBestHeight() })
                    assertEquals(0L, nodes[1].query(chainId2) { it.getBestHeight() })
                    assertEquals(0L, nodes[2].query(chainId2) { it.getBestHeight() })
                }

        // Shutting down node 0
        nodes[0].shutdown()

        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 1 is started at peers 0, 1, 2, 3
                    nodes[0].assertChainNotStarted(chainId1)
                    nodes[1].assertChainStarted(chainId1)
                    nodes[2].assertChainStarted(chainId1)
                    nodes[3].assertChainStarted(chainId1)

                    // network topology of chain 1 is that peers 0, 1, 3, 4 are interconnected
//                    nodes[0].assertNodeConnectedWith(chainId1, nodes[1], nodes[2], nodes[3])
                    nodes[1].assertNodeConnectedWith(chainId1, /*nodes[0], */nodes[2], nodes[3])
                    nodes[2].assertNodeConnectedWith(chainId1, nodes[1], /*nodes[0], */nodes[3])
                    nodes[3].assertNodeConnectedWith(chainId1, nodes[1], nodes[2]/*, nodes[0]*/)
                }

        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 2 is started at peers 0, 1, 2
                    nodes[0].assertChainNotStarted(chainId2)
                    nodes[1].assertChainStarted(chainId2)
                    nodes[2].assertChainStarted(chainId2)

                    // network topology of chain 2 is that peers 0, 1, 2 are interconnected
//                    nodes[0].assertNodeConnectedWith(chainId2, nodes[1], nodes[2])
                    nodes[1].assertNodeConnectedWith(chainId2, /*nodes[0], */nodes[2])
                    nodes[2].assertNodeConnectedWith(chainId2, nodes[1]/*, nodes[0]*/)
                }

        // TODO: [et]: Finish this test


/*
        // Building a block 1 at chain 1 via peer 1
        nodes[1].enqueueTxsAndAwaitBuiltBlock(chainId1, 1, tx1_1)
        // * Asserting height is 0 for all peers
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
//                    assertEquals(0, nodes[0].query(chainId1) { it.getBestHeight() })
                    assertEquals(1, nodes[1].query(chainId1) { it.getBestHeight() })
                    assertEquals(1, nodes[2].query(chainId1) { it.getBestHeight() })
                    assertEquals(1, nodes[3].query(chainId1) { it.getBestHeight() })
                }
*/


/*
        // Building a block 1 at chain 2 via peer 1
        nodes[1].enqueueTxsAndAwaitBuiltBlock(chainId2, 1, tx2_1)
        // * Asserting height is 0 for all peers
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
//                    assertEquals(0, nodes[0].query(chainId2) { it.getBestHeight() })
                    assertEquals(1, nodes[1].query(chainId2) { it.getBestHeight() })
                    assertEquals(1, nodes[2].query(chainId2) { it.getBestHeight() })
                }


        */

        nodes.removeAt(0)

    }


}