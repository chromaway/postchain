package net.postchain.integrationtest.reconfiguration

import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import net.postchain.StorageBuilder
import net.postchain.common.hexStringToByteArray
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.core.NODE_ID_TODO
import net.postchain.devtools.PostchainTestNode
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.getModules
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test

class SinglePeerReconfigurationTest : ReconfigurationTest() {

    @Test
    fun reconfigurationAtHeight_is_successful() {
        // Node config
        val appConfig = createAppConfig(0, 1, DEFAULT_CONFIG_FILE)
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(appConfig)
        val chainId = nodeConfigProvider.getConfiguration().activeChainIds.first().toLong()
        val blockchainRid = BLOCKCHAIN_RIDS[chainId]!!.hexStringToByteArray()

        // Chains configs
        val blockchainConfig1 = readBlockchainConfig(
                "/net/postchain/devtools/reconfiguration/single_peer/blockchain_config_1.xml")
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/devtools/reconfiguration/single_peer/blockchain_config_2.xml")

        // Wiping of database
        StorageBuilder.buildStorage(appConfig, NODE_ID_TODO, true).close()

        PostchainTestNode(nodeConfigProvider)
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
        assertk.assert(nodes[0].getModules(chainId)[0]).isInstanceOf(DummyModule1::class)

        // Asserting blockchainConfig2 with DummyModule2 is loaded
        await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    val modules = nodes[0].getModules(chainId)
                    assertk.assert(modules).isNotEmpty()
                    assertk.assert(modules.first()).isInstanceOf(DummyModule2::class)
                }

    }

}