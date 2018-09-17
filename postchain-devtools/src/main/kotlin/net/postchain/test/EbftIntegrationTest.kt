// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.test

import net.postchain.PostchainNode
import net.postchain.test.KeyPairHelper.Companion.privKeyHex
import org.junit.After

open class EbftIntegrationTest : IntegrationTest() {

    protected var chainId: Long = 0
    protected var ebftNodes: Array<PostchainTestNode> = arrayOf()

    open fun createEbftNodes(count: Int) {
        ebftNodes = Array(count) { createEBFTNode(count, it) }
    }

    private fun createEBFTNode(nodeCount: Int, nodeIndex: Int): PostchainTestNode {
        configOverrides.setProperty("messaging.privkey", privKeyHex(nodeIndex))
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodeCount))

        val config = createConfig(nodeIndex, nodeCount, DEFAULT_CONFIG_FILE)
        chainId = chainId(config) // TODO: [et]: Require invariance of $chainId for different configs
        return PostchainTestNode(config)
                .apply { startBlockchain(chainId) }
    }

    @After
    fun tearDownEbftNodes() {
        ebftNodes.forEach(PostchainNode::stopAllBlockchain)
        ebftNodes = arrayOf()
    }
}