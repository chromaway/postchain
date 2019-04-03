package net.postchain.integrationtest.reconfiguration

import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import net.postchain.devtools.PostchainTestNode
import net.postchain.integrationtest.assertChainStarted
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test

class SinglePeerReconfigurationTest : ReconfigurationTest() {

    @Test
    fun reconfigurationAtHeight_is_successful() {
        // Node config
        val nodeConfig = createConfig(0, 1, DEFAULT_CONFIG_FILE)
        val chainId = nodeConfig.getLong("activechainids")
        val blockchainRid = readBlockchainRid(nodeConfig, chainId)

        // Chains configs
        val blockchainConfig1 = readBlockchainConfig(
                "/net/postchain/reconfiguration/single_peer/blockchain_config_1.xml")
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/reconfiguration/single_peer/blockchain_config_2.xml")

        PostchainTestNode(nodeConfig)
                .apply {
                    // Adding chain1 with blockchainConfig1 with DummyModule1
                    addBlockchain(chainId, blockchainRid, blockchainConfig1)
                    // Adding chain1's blockchainConfig2 with DummyModule2
                    addConfiguration(chainId, 5, blockchainConfig2)
                    startBlockchain()
                }
                .also {
                    nodes.add(it)
                }

        // Asserting chain1 is started
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes[0].assertChainStarted(chainId)
                }

        // Asserting blockchainConfig1 with DummyModule1 is loaded
        assertk.assert(getModules(0, chainId)[0]).isInstanceOf(DummyModule1::class)

        // Asserting blockchainConfig2 with DummyModule2 is loaded
        await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    val modules = getModules(0, chainId)
                    assertk.assert(modules).isNotEmpty()
                    assertk.assert(modules.first()).isInstanceOf(DummyModule2::class)
                }

    }

}