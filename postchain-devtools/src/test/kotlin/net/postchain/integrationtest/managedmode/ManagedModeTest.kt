package net.postchain.integrationtest.managedmode

import assertk.assertions.isTrue
import net.postchain.base.PeerInfo
import net.postchain.config.SimpleDatabaseConnector
import net.postchain.config.app.AppConfigDbLayer
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.PeerNameHelper.peerName
import net.postchain.hasSize
import net.postchain.integrationtest.assertChainStarted
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Ignore
import org.junit.Test

class ManagedModeTest : IntegrationTest() {

    @Test
    fun launchingOf_SingleNodeSingleSigner_withSystemChainOnly_isSuccessful() {
        val nodeConfig = "classpath:/net/postchain/managedmode/node0.properties"
        val blockchainConfig = "/net/postchain/devtools/managedmode/blockchain_config_1.xml"

        // Creating all nodes
        createSingleNode(0, 1, nodeConfig, blockchainConfig)

        // Asserting chain 0 is started for all nodes
        nodes[0].assertChainStarted(chainId = 0L)
    }

    @Test
    @Ignore // Not Finished yet
    fun newTest() {
        val nodeConfig0 = "classpath:/net/postchain/managedmode/node0.properties"
        val nodeConfig1 = "classpath:/net/postchain/managedmode/node1.properties"
        val blockchainConfig0 = "/net/postchain/devtools/managedmode/blockchain_config_0.xml"
        val blockchainConfig1 = "/net/postchain/devtools/managedmode/blockchain_config_1.xml"

        // Creating node0
        createSingleNode(0, 1, nodeConfig0, blockchainConfig0) { appConfig, _ ->
            val dbConnector = SimpleDatabaseConnector(appConfig)
            dbConnector.withWriteConnection { connection ->
                AppConfigDbLayer(appConfig, connection).addPeerInfo(PeerInfos.peerInfo0)
            }
        }

        // Creating node1
        createSingleNode(0, 1, nodeConfig1, blockchainConfig1) { appConfig, _ ->
            val dbConnector = SimpleDatabaseConnector(appConfig)
            dbConnector.withWriteConnection { connection ->
                AppConfigDbLayer(appConfig, connection).addPeerInfo(PeerInfos.peerInfo1)
            }
        }

        // Asserting chain 0 is started for node0
        nodes[0].assertChainStarted(0L)
        assertk.assert(nodes[0].networkTopology(0L)).hasSize(0)

        // Asserting chain 0 is started for node1
        nodes[1].assertChainStarted(0L)
        assertk.assert(nodes[1].networkTopology(0L)).hasSize(0)

        // Waiting for height 5 when a new peer will be added to PeerInfos
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    assertk.assert(nodes[0].networkTopology(0L)).hasSize(1)
//                    assertk.assert(nodes[1].networkTopology(0L)).hasSize(1)
                }

        logger.error { "\n\n\n\n\n" }
        logger.error { "==================================================================== " }


        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    assertk.assert(false).isTrue()
                }


        /*
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    val peerInfos = nodes[0].nodeConfigProvider.getConfiguration().peerInfos
                    logger.error {
                        "PeerInfo (${peerInfos.size}): ${peerInfoAsString(peerInfos)}"
                    }
                    assertk.assert(false).isTrue()
                }

         */
    }

    private fun peerInfoAsString(peerInfos: Array<PeerInfo>): String {
        return peerInfos
                .map { peerName(it.pubKey) }
                .toTypedArray()
                .let { it.contentToString() }
    }
}