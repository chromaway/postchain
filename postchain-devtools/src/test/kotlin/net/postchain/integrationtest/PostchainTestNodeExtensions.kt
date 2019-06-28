package net.postchain.integrationtest

import assertk.assertions.isNotNull
import assertk.assertions.isNull
import junit.framework.Assert.assertNotNull
import net.postchain.containsExactlyKeys
import net.postchain.core.BlockQueries
import net.postchain.core.MultiSigBlockWitness
import net.postchain.core.Signature
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.gtv.Gtv
import nl.komponents.kovenant.Promise

fun PostchainTestNode.addBlockchainAndStart(chainId: Long, blockchainRid: ByteArray, blockchainConfig: Gtv) {
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

fun <T> PostchainTestNode.query(chainId: Long, action: (BlockQueries) -> Promise<T, Exception>): T? {
    return retrieveBlockchain(chainId)?.getEngine()?.getBlockQueries()?.run {
        action(this)
    }?.get()
}

fun PostchainTestNode.blockSignatures(chainId: Long, height: Long): Array<Signature> {
    val block = query(chainId) { it.getBlockAtHeight(height) }
    assertNotNull(block)
    return (block!!.witness as? MultiSigBlockWitness)
            ?.getSignatures() ?: emptyArray()
}

fun PostchainTestNode.awaitedHeight(chainId: Long): Long {
    return query(chainId) { it.getBestHeight() }
            ?.plus(1) ?: -1L
}

fun PostchainTestNode.buildBlocksUpTo(chainId: Long, height: Long) {
    val strategy = getBlockchainInstance(chainId).getEngine()
            .getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy

    strategy.buildBlocksUpTo(height)
}

fun PostchainTestNode.awaitBuiltBlock(chainId: Long, height: Long) {
    val strategy = getBlockchainInstance(chainId).getEngine()
            .getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy

    strategy.buildBlocksUpTo(height)
    strategy.awaitCommitted(height.toInt())
}

fun PostchainTestNode.enqueueTxs(chainId: Long, vararg txs: TestTransaction) {
    val txQueue = getBlockchainInstance(chainId).getEngine().getTransactionQueue()
    txs.forEach { txQueue.enqueue(it) }
}

fun PostchainTestNode.enqueueTxsAndAwaitBuiltBlock(chainId: Long, height: Long, vararg txs: TestTransaction) {
    enqueueTxs(chainId, *txs)
    awaitBuiltBlock(chainId, height)
}