package net.postchain.integrationtest.reconfiguration

import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_ID
import net.postchain.devtools.testinfra.TestBlockchainConfiguration
import net.postchain.gtx.CompositeGTXModule
import net.postchain.gtx.GTXBlockchainConfiguration
import net.postchain.gtx.GTXModule
import net.postchain.integrationtest.awaitedHeight
import net.postchain.integrationtest.buildBlocksUpTo

open class ReconfigurationTest : IntegrationTest() {

    protected fun awaitReconfiguration(height: Long) {
        nodes.first().buildBlocksUpTo(DEFAULT_CHAIN_ID, height - 1)
        while (nodes.any { it.awaitedHeight(DEFAULT_CHAIN_ID) < height });
    }

    protected fun getModules(nodeIndex: Int, chainId: Long = DEFAULT_CHAIN_ID): Array<GTXModule> {
        val configuration = nodes[nodeIndex].retrieveBlockchain(chainId)
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