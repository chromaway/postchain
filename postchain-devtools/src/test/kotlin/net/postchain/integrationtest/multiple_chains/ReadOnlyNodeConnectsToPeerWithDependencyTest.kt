// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.multiple_chains

import mu.KLogging
import net.postchain.devtools.TxCache
import net.postchain.util.GtxTxIntegrationTestSetup
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import org.junit.Ignore
import org.junit.Test


/**
 * This will test the case when a read only node will connect to a signer node where:
 *
 * 1. The signer node has chain A
 * 2. The read only node reads A and
 * 3. The read only node also has chain B where B depends on A.
 */
class ReadOnlyNodeConnectsToPeerWithDependencyTest : GtxTxIntegrationTestSetup() {

    companion object : KLogging()

    /**
     * One BC depend on another BC.
     */
    @Test
    @Ignore // TODO: Olle POS-114 should be made to work.
    fun testHappyDependency() {
        val chainList = listOf(1L, 2L)

        //val node1readOnly = NodeNameWithBlockchains("classpath:/net/postchain/multiple_chains/dependent_bcs/read_only_with_peer/node1bc2dep1.properties"
        val bcFileMap =   mapOf(
                   1 to "/net/postchain/devtools/multiple_chains/dependent_bcs/read_only_with_peer/blockchain_config_1.xml",
                   2 to "/net/postchain/devtools/multiple_chains/dependent_bcs/read_only_with_peer/blockchain_config_2_depends_on_1.xml"
                )

        //val nodeNameWithBlockchainsArr = arrayOf(node0, node1readOnly)
        val systemSetup = SystemSetupFactory.buildSystemSetup(bcFileMap)

        runXNodes (systemSetup)

        val txCache = TxCache(mutableMapOf())
        runXNodesWithYTxPerBlock( 2, 10, systemSetup, txCache)
        runXNodesAssertions( 2, 10, systemSetup, txCache)

    }


}
