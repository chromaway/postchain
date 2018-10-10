// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.test

import net.postchain.PostchainNode
import net.postchain.test.KeyPairHelper.privKeyHex
import org.junit.After

open class EbftIntegrationTest : IntegrationTest() {

    protected var ebftNodes: Array<SingleChainTestNode> = arrayOf()

    open fun createEbftNodes(count: Int) {
        ebftNodes = Array(count) { createEBFTNode(it, count) }
    }

    private fun createEBFTNode(nodeIndex: Int, nodeCount: Int): SingleChainTestNode {
        configOverrides.setProperty("messaging.privkey", privKeyHex(nodeIndex))
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodeCount))

        val config = createConfig(nodeIndex, nodeCount, DEFAULT_CONFIG_FILE)
        return SingleChainTestNode(config)
                .apply { startBlockchain() }
    }

    @After
    fun tearDownEbftNodes() {
        ebftNodes.forEach(PostchainNode::stopAllBlockchain)
        ebftNodes = arrayOf()
    }
}