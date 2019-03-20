package net.postchain.integrationtest.reconnection

import assertk.assertions.isNotNull
import assertk.assertions.isNull
import net.postchain.devtools.PostchainTestNode
import net.postchain.hasSize
import net.postchain.isEmpty
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Test

class TwoPeersReconnectionTest : ReconnectionTest() {

    @Test
    fun test2Peers() {
        val nodesCount = 2
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val blockchainConfig = "/net/postchain/stability/blockchain_config_2.xml"
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/stability/node0.properties",
                "classpath:/net/postchain/stability/node1.properties"
        )

        // Creating all nodes
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig)
        }

        // Asserting that chain is started
        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()
                    assertk.assert(nodes[1].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()
                }

        // Printing net networkTopology
//        printPeers(0, 1)

        // Asserting network topology is pair of connected peers
        assertk.assert(nodes[0].networkTopology()).hasSize(1)
        assertk.assert(nodes[1].networkTopology()).hasSize(1)

        // Shutting down peer 1
        nodes[1].shutdown()

        // Asserting that
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    //                    printPeers(0, 1)

                    // chain is active for peer 0 and is shutdown for peer 1
                    assertk.assert(nodes[0].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()
                    assertk.assert(nodes[1].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNull()

                    // network topology is pair of disconnected peers
                    assertk.assert(nodes[0].networkTopology()).isEmpty()
                }

        // Removing node 1
        nodes.removeAt(1)

        println("Re-boring peer 1")
        // Re-boring peer 1
        createSingleNode(1, nodesCount, nodeConfigsFilenames[1], blockchainConfig)

        // Asserting that
        Awaitility.await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // Asserting that chain is started at peer 1
                    assertk.assert(nodes[1].retrieveBlockchain(PostchainTestNode.DEFAULT_CHAIN_ID)).isNotNull()

                    // network topology is pair of connected peers
                    assertk.assert(nodes[0].networkTopology()).hasSize(1)
                    assertk.assert(nodes[1].networkTopology()).hasSize(1)
                }
    }
}