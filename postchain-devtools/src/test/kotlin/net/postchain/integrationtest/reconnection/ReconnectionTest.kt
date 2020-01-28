// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.reconnection

import assertk.assertions.containsExactly
import assertk.assertions.isFalse
import net.postchain.core.BlockQueries
import net.postchain.core.Transaction
import net.postchain.core.byteArrayKeyOf
import net.postchain.devtools.ConfigFileBasedIntegrationTest
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.testinfra.TestTransaction
import nl.komponents.kovenant.Promise
import java.util.*

open class ReconnectionTest : ConfigFileBasedIntegrationTest() {

    protected val tx0 = TestTransaction(0)
    protected val tx1 = TestTransaction(1)
    protected val tx10 = TestTransaction(10)
    protected val tx11 = TestTransaction(11)
    protected val tx100 = TestTransaction(100)
    protected val tx101 = TestTransaction(101)

    protected fun enqueueTransactions(node: PostchainTestNode, vararg txs: TestTransaction) {
        val txQueue = node.getBlockchainInstance().getEngine().getTransactionQueue()
        txs.forEach { txQueue.enqueue(it) }
    }

    protected fun awaitBuiltBlock(node: PostchainTestNode, height: Long) {
        val strategy = node
                .getBlockchainInstance()
                .getEngine()
                .getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy

        strategy.buildBlocksUpTo(height)
        strategy.awaitCommitted(height.toInt())
    }

    protected fun <T> queries(node: PostchainTestNode, action: (BlockQueries) -> Promise<T, Exception>): T {
        return node
                .getBlockchainInstance()
                .getEngine()
                .getBlockQueries()
                .run {
                    action(this)
                }.get()
    }

    protected fun assertThatNodeInBlockHasTxs(node: PostchainTestNode, height: Long, vararg txs: Transaction) {
        // Asserting number of blocks at height
        val blockRids = queries(node) { it.getBlockRid(height) }
        assertk.assert(blockRids == null).isFalse()

        // Asserting content of a block
        val txsRids = queries(node) { it.getBlockTransactionRids(blockRids!!) }.map(ByteArray::byteArrayKeyOf)
        assertk.assert(txsRids).containsExactly(
                *txs.map { tx -> tx.getRID().byteArrayKeyOf() }.toTypedArray())
    }

    @SuppressWarnings("unused")
    protected fun printPeer(index: Int) {
        try {
            nodes[index].networkTopology()
                    .map { (pubKey, connection) ->
                        "$connection:${nodesNames[pubKey]}"
                    }
                    .let { peers ->
                        println("Node $index: ${Arrays.toString(peers.toTypedArray())}")
                    }
        } catch (e: java.lang.Exception) {
            println("printPeer($index): $e")
        }
    }

}