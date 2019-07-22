package net.postchain.integrationtest.managedmode

import net.postchain.devtools.IntegrationTest
import net.postchain.integrationtest.assertChainStarted
import org.junit.Test

class ManagedModeTest : IntegrationTest() {

    @Test
    fun launchingOf_SingleNodeSingleSigner_withSystemChainOnly_isSuccessful() {
        val nodeConfig = "classpath:/net/postchain/managedmode/node0.properties"
        val blockchainConfig = "/net/postchain/managedmode/blockchain_config_1.xml"

        // Creating all nodes
        createSingleNode(0, 1, nodeConfig, blockchainConfig)

        // Asserting chain 1 is started for all nodes
        nodes[0].assertChainStarted(chainId = 0L)
    }
}