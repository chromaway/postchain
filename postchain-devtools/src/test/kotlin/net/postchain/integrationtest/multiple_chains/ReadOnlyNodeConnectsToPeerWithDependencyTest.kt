package net.postchain.integrationtest.multiple_chains

import mu.KLogging
import net.postchain.devtools.utils.configuration.NodeChain
import net.postchain.util.MultiNodeDoubleChainBlockTestHelper
import net.postchain.devtools.utils.configuration.NodeNameWithBlockchains
import net.postchain.devtools.TxCache
import org.junit.Ignore
import org.junit.Test


/**
 * This will test the case when a read only node will connect to a signer node where:
 *
 * 1. The signer node has chain A
 * 2. The read only node reads A and
 * 3. The read only node also has chain B where B depends on A.
 */
class ReadOnlyNodeConnectsToPeerWithDependencyTest : MultiNodeDoubleChainBlockTestHelper() {

    companion object : KLogging()

    /**
     * One BC depend on another BC.
     */
    @Test
    fun testHappyDependency() {
        val chainList = listOf(1L, 2L)

        val node0 = NodeNameWithBlockchains("classpath:/net/postchain/multiple_chains/dependent_bcs/read_only_with_peer/node0bc1.properties"
                , listOf(NodeChain("/net/postchain/multiple_chains/dependent_bcs/read_only_with_peer/blockchain_config_1.xml", 1L))
        )

        val node1readOnly = NodeNameWithBlockchains("classpath:/net/postchain/multiple_chains/dependent_bcs/read_only_with_peer/node1bc2dep1.properties"
                , listOf(
                   NodeChain("/net/postchain/multiple_chains/dependent_bcs/read_only_with_peer/blockchain_config_1.xml", 1L, true),
                   NodeChain("/net/postchain/multiple_chains/dependent_bcs/read_only_with_peer/blockchain_config_2_depends_on_1.xml", 2L)
                )
        )

        val nodeNameWithBlockchainsArr = arrayOf(node0, node1readOnly)

        runXNodesWithDifferentNumberOfChainsPerNode (2, nodeNameWithBlockchainsArr, listOf(1))

        val txCache = TxCache(mutableMapOf())
        runXNodesWithYTxPerBlock( 2, 10, chainList, txCache, nodeNameWithBlockchainsArr)
        runXNodesAssertions( 2, 10, chainList, txCache, nodeNameWithBlockchainsArr)

    }


}
