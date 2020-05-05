// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.multiple_chains

import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.base.BlockchainRid
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.core.NODE_ID_TODO
import net.postchain.devtools.ConfigFileBasedIntegrationTest
import net.postchain.devtools.PostchainTestNode
import org.junit.Test

class SinglePeerDoubleChainsDependencyTest : ConfigFileBasedIntegrationTest() {

    companion object : KLogging() {

        const val BAD_DEPENDENCY_RID = "ABABABABABABABABABABABABABABABABABABABABABABABABABABABABABABABAB" // The dummy RID that's in the config file.
    }

    /**
     * What if our configuration tells us we should have a dependency, but we haven't got it?
     */
    @Test
    fun testBreakIfDependencyNotFound() {
        // Building configs
        val nodeConfigFilename = "classpath:/net/postchain/multiple_chains/dependent_bcs/single_peer/node0bc1dep.properties"
        val blockchainConfigFilename = "/net/postchain/devtools/multiple_chains/dependent_bcs/single_peer/blockchain_config_bad_dependency.xml"
        configOverrides.setProperty("testpeerinfos", createPeerInfos(1))
        val appConfig = createAppConfig(0, 1, nodeConfigFilename)
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(appConfig)

        StorageBuilder.buildStorage(appConfig, NODE_ID_TODO, true).close()

        // Building a PostchainNode
        val node = PostchainTestNode(nodeConfigProvider)
                .also { nodes.add(it) }

        // Launching blockchain
        val blockchainConfig = readBlockchainConfig(blockchainConfigFilename)
        val blockchainRid = node.addBlockchain(1L, blockchainConfig)
        assertk.assert {
            node.startBlockchain(1L)
        }.returnedValue { null }
    }



}

