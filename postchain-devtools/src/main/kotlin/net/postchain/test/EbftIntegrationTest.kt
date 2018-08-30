// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.test

import net.postchain.LegacyTestNode
import org.junit.After

open class EbftIntegrationTest : IntegrationTest() {
    protected var ebftNodes: Array<LegacyTestNode> = arrayOf()

    open fun createEbftNodes(count: Int) {
        ebftNodes = Array(count) { createEBFTNode(count, it) }
    }

    private fun createEBFTNode(nodeCount: Int, myIndex: Int): LegacyTestNode {
        configOverrides.setProperty("messaging.privkey", privKeyHex(myIndex))
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodeCount))
        val pn = LegacyTestNode()
        pn.start(createConfig(myIndex, nodeCount, DEFAULT_CONFIG_FILE), myIndex)
        return pn
    }

    @After
    fun tearDownEbftNodes() {
        ebftNodes.forEach {
            it.stop()
        }
        ebftNodes = arrayOf()
    }
}