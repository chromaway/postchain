package net.postchain.integrationtest

import assertk.assertions.isNotNull
import assertk.assertions.isNull
import net.postchain.containsExactlyKeys
import net.postchain.devtools.PostchainTestNode
import net.postchain.gtx.GTXValue

fun PostchainTestNode.addBlockchainAndStart(chainId1: Long, blockchainRid: ByteArray, blockchainConfig: GTXValue) {
    addBlockchain(chainId1, blockchainRid, blockchainConfig)
    startBlockchain(chainId1)
}

fun PostchainTestNode.assertChainStarted(chainId: Long = PostchainTestNode.DEFAULT_CHAIN_ID) {
    assertk.assert(retrieveBlockchain(chainId)).isNotNull()
}

fun PostchainTestNode.assertChainNotStarted(chainId: Long = PostchainTestNode.DEFAULT_CHAIN_ID) {
    assertk.assert(retrieveBlockchain(chainId)).isNull()
}

fun PostchainTestNode.assertNodeConnectedWith(chainId: Long, vararg nodes: PostchainTestNode) {
    assertk.assert(networkTopology(chainId)).containsExactlyKeys(
            *nodes.map(PostchainTestNode::pubKey).toTypedArray()
    )
}

