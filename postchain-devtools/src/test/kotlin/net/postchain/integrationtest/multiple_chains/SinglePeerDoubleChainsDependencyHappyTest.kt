package net.postchain.integrationtest.multiple_chains

import mu.KLogging
import net.postchain.devtools.TxCache
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.util.GtxTxIntegrationTestSetup
import org.junit.Test

class SinglePeerDoubleChainsDependencyHappyTest : GtxTxIntegrationTestSetup() {

    companion object : KLogging()

    /**
     * Begin with a simple happy test to see that we can start/stop a node with 2 chains.
     */
    @Test
    fun startingAndStoppingSingleChainSuccessfully() {
        val mapBcFiles = mapOf(
                        1 to "/net/postchain/devtools/multiple_chains/dependent_bcs/single_peer/blockchain_config_1.xml",
                        2 to "/net/postchain/devtools/multiple_chains/dependent_bcs/single_peer/blockchain_config_2.xml"
                )

        val sysSetup = SystemSetupFactory.buildSystemSetup(mapBcFiles)

        runXNodes(sysSetup)

        val txCache = TxCache(mutableMapOf())
        runXNodesWithYTxPerBlock(1, 1, sysSetup, txCache)
        runXNodesAssertions(1, 1, sysSetup, txCache)
    }

    /**
     * One BC depend on another BC.
     */
    @Test
    fun testHappyDependency() {
        val mapBcFiles = mapOf(
                        1 to "/net/postchain/devtools/multiple_chains/dependent_bcs/single_peer/blockchain_config_1.xml",
                        2 to "/net/postchain/devtools/multiple_chains/dependent_bcs/single_peer/blockchain_config_2_depends_on_1.xml"
                )

        val sysSetup = SystemSetupFactory.buildSystemSetup(mapBcFiles)

        runXNodes(sysSetup)

        val txCache = TxCache(mutableMapOf())
        runXNodesWithYTxPerBlock(2, 10, sysSetup, txCache)
        runXNodesAssertions(2, 10, sysSetup, txCache)

    }
}