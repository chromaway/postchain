// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.reconnection

import net.postchain.hasSize
import net.postchain.devtools.assertChainNotStarted
import net.postchain.devtools.assertChainStarted
import net.postchain.devtools.assertNodeConnectedWith
import net.postchain.isEmpty
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Test

// TODO: this test seems flaky, investigate
class TwoPeersReconnectionTest : ReconnectionTest() {

    @Test
    fun test2Peers() {
        val nodesCount = 2
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val blockchainConfig = "/net/postchain/devtools/reconnection/blockchain_config_2.xml"
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/reconnection/node0.properties",
                "classpath:/net/postchain/reconnection/node1.properties"
        )

        // Creating all peers
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig)
        }

        // Asserting that chain is started
        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    nodes[0].assertChainStarted()
                    nodes[1].assertChainStarted()
                }
        //println("---- Both started ------")

        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    nodes[0].assertNodeConnectedWith(1, nodes[1])
                    nodes[1].assertNodeConnectedWith(1, nodes[0])
                }
        //println("---- Both connected ------")


        // Asserting network topology is pair of connected peers
        assertk.assert(nodes[0].networkTopology()).hasSize(1)
        assertk.assert(nodes[1].networkTopology()).hasSize(1)
        //println("---- Both asserted ------")

        // Shutting down peer 1
        nodes[1].shutdown()

        // Asserting that
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    //                    printPeers(0, 1)

                    // chain is active for peer 0 and is shutdown for peer 1
                    nodes[0].assertChainStarted()
                    nodes[1].assertChainNotStarted()

                    // network topology is pair of disconnected peers
                    assertk.assert(nodes[0].networkTopology()).isEmpty()
                }

        // Removing peer 1
        nodes.removeAt(1)

        println("Re-boring peer 1")
        createSingleNode(1, nodesCount, nodeConfigsFilenames[1], blockchainConfig)

        // Asserting that
        Awaitility.await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // Asserting that chain is started at peer 1
                    nodes[1].assertChainStarted()

                    // network topology is pair of connected peers
                    assertk.assert(nodes[0].networkTopology()).hasSize(1)
                    assertk.assert(nodes[1].networkTopology()).hasSize(1)
                }
    }
}