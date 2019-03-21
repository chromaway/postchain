package net.postchain.integrationtest.reconfiguration

import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_ID
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test

class FourPeersReconfigurationTest : ReconfigurationTest() {

    @Test
    fun reconfigurationAtHeight_is_successful() {
        val nodesCount = 4
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/reconfiguration/node0.properties",
                "classpath:/net/postchain/reconfiguration/node1.properties",
                "classpath:/net/postchain/reconfiguration/node2.properties",
                "classpath:/net/postchain/reconfiguration/node3.properties"
        )

        // Chains configs
        val blockchainConfig1 = "/net/postchain/reconfiguration/four_peers/blockchain_config_1.xml"
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/reconfiguration/four_peers/blockchain_config_2.xml")
        val blockchainConfig3 = readBlockchainConfig(
                "/net/postchain/reconfiguration/four_peers/blockchain_config_3.xml")

        // Creating all peers
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig1)
        }

        // Asserting chain 1 is started for all peers
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain()).isNotNull()
                    assertk.assert(nodes[1].retrieveBlockchain()).isNotNull()
                    assertk.assert(nodes[2].retrieveBlockchain()).isNotNull()
                    assertk.assert(nodes[3].retrieveBlockchain()).isNotNull()
                }

        // Asserting blockchainConfig1 with DummyModule1 is loaded y all peers
        assertk.assert(getModules(0).first()).isInstanceOf(DummyModule1::class)
        assertk.assert(getModules(1).first()).isInstanceOf(DummyModule1::class)
        assertk.assert(getModules(2).first()).isInstanceOf(DummyModule1::class)
        assertk.assert(getModules(3).first()).isInstanceOf(DummyModule1::class)

        // Adding chain1's blockchainConfig2 with DummyModule2 at different heights to all peers
        nodes[0].addConfiguration(DEFAULT_CHAIN_ID, 2, blockchainConfig2)
        nodes[1].addConfiguration(DEFAULT_CHAIN_ID, 3, blockchainConfig2)
        nodes[2].addConfiguration(DEFAULT_CHAIN_ID, 4, blockchainConfig2)
        nodes[3].addConfiguration(DEFAULT_CHAIN_ID, 5, blockchainConfig2)

        // Asserting blockchainConfig2 with DummyModule2 is loaded by all peers
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(getModules(0)).isNotEmpty()
                    assertk.assert(getModules(1)).isNotEmpty()
                    assertk.assert(getModules(2)).isNotEmpty()
                    assertk.assert(getModules(3)).isNotEmpty()

                    assertk.assert(getModules(0).first()).isInstanceOf(DummyModule2::class)
                    assertk.assert(getModules(1).first()).isInstanceOf(DummyModule2::class)
                    assertk.assert(getModules(2).first()).isInstanceOf(DummyModule2::class)
                    assertk.assert(getModules(3).first()).isInstanceOf(DummyModule2::class)
                }

        // Again: Adding chain1's blockchainConfig3 with DummyModule3 at height 7 to all peers
        nodes[0].addConfiguration(DEFAULT_CHAIN_ID, 7, blockchainConfig3)
        nodes[1].addConfiguration(DEFAULT_CHAIN_ID, 7, blockchainConfig3)
        nodes[2].addConfiguration(DEFAULT_CHAIN_ID, 7, blockchainConfig3)
        nodes[3].addConfiguration(DEFAULT_CHAIN_ID, 7, blockchainConfig3)

        // Asserting blockchainConfig2 with DummyModule2 is loaded by all peers
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(getModules(0)).isNotEmpty()
                    assertk.assert(getModules(1)).isNotEmpty()
                    assertk.assert(getModules(2)).isNotEmpty()
                    assertk.assert(getModules(3)).isNotEmpty()

                    assertk.assert(getModules(0).first()).isInstanceOf(DummyModule3::class)
                    assertk.assert(getModules(1).first()).isInstanceOf(DummyModule3::class)
                    assertk.assert(getModules(2).first()).isInstanceOf(DummyModule3::class)
                    assertk.assert(getModules(3).first()).isInstanceOf(DummyModule3::class)
                }
    }

}