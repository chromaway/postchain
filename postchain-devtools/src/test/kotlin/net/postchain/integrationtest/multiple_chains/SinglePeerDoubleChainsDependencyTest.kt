package net.postchain.integrationtest.multiple_chains

import mu.KLogging
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.utils.configuration.BlockchainSetupFactory
import net.postchain.devtools.utils.configuration.NodeConfigurationProviderGenerator
import net.postchain.devtools.utils.configuration.NodeSeqNumber
import net.postchain.devtools.utils.configuration.SystemSetup
import net.postchain.util.RealGtxTxIntegrationTest
import org.junit.Test

class SinglePeerDoubleChainsDependencyTest : RealGtxTxIntegrationTest() {

    companion object : KLogging()

    /**
     * What if our configuration tells us we should have a dependency, but we haven't got it?
     */
    @Test
    fun testBreakIfDependencyNotFound() {
        // Building configs
        val blockchainConfigFilename = "/net/postchain/devtools/multiple_chains/dependent_bcs/single_peer/blockchain_config_bad_dependency.xml"
        configOverrides.setProperty("testpeerinfos", createPeerInfos(1))

        val bcSetup = BlockchainSetupFactory.buildFromFile(1, blockchainConfigFilename)

        val sysSetup = SystemSetup.buildComplexSetup(listOf(bcSetup))
        val nodeSetup = sysSetup.nodeMap[NodeSeqNumber(0)]!!

        val nodeConfigProvider = NodeConfigurationProviderGenerator.buildFromSetup(
                "SinglePeerDoubleChainsDependencyTest", configOverrides, nodeSetup, sysSetup)

        val node = PostchainTestNode(nodeConfigProvider)
                .also { nodes.add(it) }

        // Launching blockchain
        val blockchainRid = node.addBlockchain(1L, bcSetup.bcGtv)
        assertk.assert {
            node.startBlockchain(1L)
        }.returnedValue { null }
    }



}

