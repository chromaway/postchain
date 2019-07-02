package net.postchain.integrationtest.reconfiguration

import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_IID
import net.postchain.gtx.CompositeGTXModule
import net.postchain.gtx.GTXBlockchainConfiguration
import net.postchain.gtx.GTXModule

open class ReconfigurationTest : IntegrationTest() {

    protected fun getModules(nodeIndex: Int, chainId: Long = DEFAULT_CHAIN_IID): Array<GTXModule> {
        val configuration = nodes[nodeIndex].retrieveBlockchain(chainId)
                ?.getEngine()
                ?.getConfiguration()
                as? GTXBlockchainConfiguration

        return (configuration?.module as? CompositeGTXModule)?.modules ?: emptyArray()
    }
}