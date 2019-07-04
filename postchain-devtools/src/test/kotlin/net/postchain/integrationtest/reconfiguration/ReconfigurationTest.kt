package net.postchain.integrationtest.reconfiguration

import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_ID
import net.postchain.devtools.testinfra.TestBlockchainConfiguration
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.devtools.testinfra.TestTransactionFactory
import net.postchain.gtx.CompositeGTXModule
import net.postchain.gtx.GTXBlockchainConfiguration
import net.postchain.gtx.GTXModule
import net.postchain.integrationtest.awaitedHeight
import net.postchain.integrationtest.buildBlocksUpTo
import net.postchain.integrationtest.query
import kotlin.test.assertNotNull

open class ReconfigurationTest : IntegrationTest() {

    protected fun tx(id: Int): TestTransaction = TestTransaction(id)

    protected fun blockTxsIds(node: PostchainTestNode, height: Long): Set<Int> {
        val blockRids = node.query(DEFAULT_CHAIN_ID) { it.getBlockRids(height) }
        assertNotNull(blockRids)

        val txsRids = node.query(DEFAULT_CHAIN_ID) { it.getBlockTransactionRids(blockRids!!) }
        assertNotNull(txsRids)

        val txFactory = TestTransactionFactory()
        return txsRids!!.asSequence().map { txRid ->
            val tx = node.query(DEFAULT_CHAIN_ID) { it.getTransaction(txRid) }
            assertNotNull(tx)

            (txFactory.decodeTransaction(tx!!.getRawData()) as TestTransaction).id
        }.toSet()
    }

    protected fun awaitReconfiguration(height: Long) {
        nodes.first().buildBlocksUpTo(DEFAULT_CHAIN_ID, height - 1)
        while (nodes.any { it.awaitedHeight(DEFAULT_CHAIN_ID) < height });
    }

    protected fun getModules(nodeIndex: Int, chainId: Long = DEFAULT_CHAIN_ID): Array<GTXModule> =
            getModules(nodes[nodeIndex], chainId)

    protected fun getModules(node: PostchainTestNode, chainId: Long = DEFAULT_CHAIN_ID): Array<GTXModule> {
        val configuration = node.retrieveBlockchain(chainId)
                ?.getEngine()
                ?.getConfiguration()

        return when (configuration) {
            is GTXBlockchainConfiguration -> readModules(configuration.module)
            is TestBlockchainConfiguration -> readModules(configuration.module)
            else -> emptyArray()
        }
    }

    private fun readModules(compositeModule: GTXModule): Array<GTXModule> {
        return (compositeModule as? CompositeGTXModule)?.modules ?: emptyArray()
    }
}