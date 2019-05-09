package net.postchain.integrationtest

import assertk.assertions.isNotNull
import assertk.assertions.isNull
import net.postchain.containsExactlyKeys
import net.postchain.core.BlockQueries
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.gtx.GTXValue
import nl.komponents.kovenant.Promise

fun PostchainTestNode.addBlockchainAndStart(chainId: Long, blockchainRid: ByteArray, blockchainConfig: GTXValue) {
    addBlockchain(chainId, blockchainRid, blockchainConfig)
    startBlockchain(chainId)
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

fun <T> PostchainTestNode.query(chainId: Long, action: (BlockQueries) -> Promise<T, Exception>): T {
    return getBlockchainInstance(chainId).getEngine().getBlockQueries().run {
        action(this)
    }.get()
}

fun PostchainTestNode.enqueueTxsAndAwaitBuiltBlock(chainId: Long, height: Long, vararg txs: TestTransaction) {
    val txQueue = getBlockchainInstance(chainId).getEngine().getTransactionQueue()
    txs.forEach { txQueue.enqueue(it) }

    val strategy = getBlockchainInstance(chainId).getEngine()
            .getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy

    strategy.buildBlocksUpTo(height)
    strategy.awaitCommitted(height.toInt())
}