package net.postchain.integrationtest.reconnection

import net.postchain.integrationtest.assertChainNotStarted
import net.postchain.integrationtest.assertChainStarted
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Assert
import org.junit.Test

class SinglePeerReconnectionTest : ReconnectionTest() {

    @Test
    fun testSinglePeer() {
        val nodesCount = 1
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val blockchainConfig = "/net/postchain/devtools/reconnection/blockchain_config_1.xml"

        // Creating all peers
        createSingleNode(
                0,
                nodesCount,
                "classpath:/net/postchain/reconnection/node0.properties",
                blockchainConfig)

        // Asserting that chain is started
        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    nodes[0].assertChainStarted()
                }

        // Asserting height is -1 for all peers
        Assert.assertEquals(-1, queries(nodes[0]) { it.getBestHeight() })

        // Building a block 0
        nodes[0].let {
            enqueueTransactions(it, tx0, tx1)
            awaitBuiltBlock(it, 0)
        }
        // * Asserting height is 0 for all peers
        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    Assert.assertEquals(0, queries(nodes[0]) { it.getBestHeight() })
                }

        // Again: Building a block 1
        nodes[0].let {
            enqueueTransactions(it, tx10, tx11)
            awaitBuiltBlock(it, 1)
        }
        // * Asserting height is 1 for all peers
        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    Assert.assertEquals(1, queries(nodes[0]) { it.getBestHeight() })
                }

        // Shutting down peer 0
        nodes[0].shutdown()

        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    nodes[0].assertChainNotStarted()
                }

        nodes.removeAt(0)
    }

}