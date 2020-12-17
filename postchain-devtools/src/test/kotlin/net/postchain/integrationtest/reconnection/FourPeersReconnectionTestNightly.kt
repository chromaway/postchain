package net.postchain.integrationtest.reconnection

import net.postchain.devtools.KeyPairHelper
import net.postchain.integrationtest.assertChainStarted
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Before
import org.junit.Test

class FourPeersReconnectionTestNightly : FourPeersReconnectionImpl() {

    private val nodeIndexShift = 10

    @Before
    fun setUp() {
        reset()
    }

    private fun mapNodeId(nodeId: Int): Int = nodeId + nodeIndexShift

    override fun generatePubKey(nodeId: Int): ByteArray = KeyPairHelper.pubKey(mapNodeId(nodeId))

    @Test
    fun test4Peers() {
        val nodesCount = 4
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val blockchainConfig = "/net/postchain/devtools/reconnection/blockchain_config_4.xml"
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/reconnection/four/node0.properties",
                "classpath:/net/postchain/reconnection/four/node1.properties",
                "classpath:/net/postchain/reconnection/four/node2.properties",
                "classpath:/net/postchain/reconnection/four/node3.properties"
        )

        // Creating all peers
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(mapNodeId(i), nodesCount, nodeConfig, blockchainConfig)
        }

        // Asserting that chain is started
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    nodes.forEach { it.assertChainStarted() }
                }

        // Asserting height is -1 for all peers
        assertHeightForAllNodes(-1)

        // Building 6 blocks
        buildNonEmptyBlocks(-1, 5)

        // Shutting down node 3
        nodes[3].shutdown()

        // Asserting that node3 is stopped
        assertChainStarted(true, true, true, false)
        assertTopology(0, 1, 2)

        // Removing peer 3
        nodes.removeAt(3)

        // Building additional 6 blocks
        buildNonEmptyBlocks(5, 11)

        println("Stating peer 3 ...")
        createSingleNode(mapNodeId(3), nodesCount, nodeConfigsFilenames[1], blockchainConfig)

        // Asserting that node3 is a part of network
        assertChainStarted(true, true, true, true)
        assertTopology(0, 1, 2, 3)

        // Building additional 6 blocks
        buildNonEmptyBlocks(11, 23)
    }
}