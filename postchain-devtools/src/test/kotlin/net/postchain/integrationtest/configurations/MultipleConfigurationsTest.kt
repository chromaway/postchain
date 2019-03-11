package net.postchain.integrationtest.configurations

import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.PostchainTestNode
import net.postchain.gtx.CompositeGTXModule
import net.postchain.gtx.GTXBlockchainConfiguration
import net.postchain.gtx.GTXModule
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test

class MultipleConfigurationsTest : IntegrationTest() {

    @Test
    fun reconfigurationAtHeight_is_successful() {
        // Node config
        val nodeConfig = createConfig(0, 1, DEFAULT_CONFIG_FILE)
        val chainId = nodeConfig.getLong("activechainids")
        val blockchainRid = readBlockchainRid(nodeConfig, chainId)

        // Chains configs
        val blockchainConfig1 = readBlockchainConfig(
                "/net/postchain/multiple_configurations/blockchain_config_1.xml")
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/multiple_configurations/blockchain_config_2.xml")

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
                    assertk.assert(nodes[0].retrieveBlockchain(chainId)).isNotNull()
                }

        // Asserting blockchainConfig1 with DummyModule1 is loaded
        assertk.assert(getModules(chainId)[0]).isInstanceOf(DummyModule1::class)

        // Asserting blockchainConfig2 with DummyModule2 is loaded
        await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    val modules = getModules(chainId)
                    assertk.assert(modules).isNotEmpty()
                    assertk.assert(modules.first()).isInstanceOf(DummyModule2::class)
                }

    }

    private fun getModules(chainId: Long): Array<GTXModule> {
        val configuration = nodes[0].retrieveBlockchain(chainId)?.blockchainConfiguration as? GTXBlockchainConfiguration
        return (configuration?.module as? CompositeGTXModule)?.modules ?: emptyArray()
    }

}