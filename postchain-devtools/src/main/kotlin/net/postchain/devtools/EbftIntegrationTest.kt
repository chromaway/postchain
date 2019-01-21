// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.devtools

open class EbftIntegrationTest : IntegrationTest() {

    open fun createEbftNodes(count: Int) {
        configOverrides.setProperty("testpeerinfos", createPeerInfos(count))
        createNodes(count, "/gtx_it/blockchain_config.xml")
    }

    /*
    private fun createEBFTNode(nodeIndex: Int, nodeCount: Int): SingleChainTestNode {
        configOverrides.setProperty("messaging.privkey", privKeyHex(nodeIndex))
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodeCount))

        val config = createConfig(nodeIndex, nodeCount, DEFAULT_CONFIG_FILE)
        return SingleChainTestNode(config, DEFAULT_BLOCKCHAIN_CONFIG_FILE)
                .apply { startBlockchain() }
    }
    */

    /*
    @After
    fun tearDownEbftNodes() {
        ebftNodes.forEach(PostchainNode::stopAllBlockchain)
        ebftNodes = arrayOf()
    }
    */
}