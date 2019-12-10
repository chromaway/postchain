package net.postchain.integrationtest.multiple_chains

import assertk.assertions.isFalse
import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.common.hexStringToByteArray
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.core.NODE_ID_TODO
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.TxCache
import net.postchain.util.MultiNodeDoubleChainBlockTestHelper
import org.junit.Test

class SinglePeerDoubleChainsDependencyTest : MultiNodeDoubleChainBlockTestHelper() {

    companion object : KLogging()

    /**
     * Begin with a simple happy test to see that we can start/stop a node with 2 chains.
     */
    @Test
    fun startingAndStoppingSingleChainSuccessfully() {
        val chainList = listOf(1L, 2L)
        runXNodes(
                1,
                chainList,
                arrayOf(
                        "classpath:/net/postchain/multiple_chains/dependent_bcs/single_peer/node0bc2dep1.properties"
                ),
                arrayOf(
                        "/net/postchain/devtools/multiple_chains/dependent_bcs/single_peer/blockchain_config_1.xml",
                        "/net/postchain/devtools/multiple_chains/dependent_bcs/single_peer/blockchain_config_2.xml"
                )
        )
        val txCache = TxCache(mutableMapOf())
        runXNodesWithYTxPerBlock(1, 1, chainList, txCache)
        runXNodesAssertions(1, 1, chainList, txCache)
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

    /**
     * One BC depend on another BC.
     */
    @Test
    fun testHappyDependency() {
        val chainList = listOf(1L, 2L)

        runXNodes(
                1,
                chainList,
                arrayOf(
                        "classpath:/net/postchain/multiple_chains/dependent_bcs/single_peer/node0bc2dep1.properties"
                ),
                arrayOf(
                        "/net/postchain/devtools/multiple_chains/dependent_bcs/single_peer/blockchain_config_1.xml",
                        "/net/postchain/devtools/multiple_chains/dependent_bcs/single_peer/blockchain_config_2_depends_on_1.xml"
                )
        )

        val txCache = TxCache(mutableMapOf())
        runXNodesWithYTxPerBlock(2, 10, chainList, txCache)
        runXNodesAssertions(2, 10, chainList, txCache)

    }

}

