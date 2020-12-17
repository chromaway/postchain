package net.postchain.integrationtest.reconnection

import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_IID
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.integrationtest.assertChainNotStarted
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.assertNodeConnectedWith
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Assert
import kotlin.random.Random

open class FourPeersReconnectionImpl : ReconnectionTest() {

    private var builtBlocksCount = 0L
    protected fun reset() {
        builtBlocksCount = 0
    }

    fun assertHeightForAllNodes(height: Long) {
        Awaitility.await().atMost(Duration.TEN_SECONDS.multiply(3))
                .untilAsserted {
                    Assert.assertEquals(height, queries(nodes[0]) { it.getBestHeight() })
                    Assert.assertEquals(height, queries(nodes[1]) { it.getBestHeight() })
                    Assert.assertEquals(height, queries(nodes[2]) { it.getBestHeight() })
                    Assert.assertEquals(height, queries(nodes[3]) { it.getBestHeight() })
                }
    }

    /**
     * Asserts that chain is started (or not) for each node
     */
    fun assertChainStarted(vararg started: Boolean) {
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    nodes[0].run {
                        if (started[0]) assertChainStarted() else assertChainNotStarted()
                    }

                    nodes[1].run {
                        if (started[1]) assertChainStarted() else assertChainNotStarted()
                    }

                    nodes[2].run {
                        if (started[2]) assertChainStarted() else assertChainNotStarted()
                    }

                    nodes[3].run {
                        if (started[3]) assertChainStarted() else assertChainNotStarted()
                    }
                }
    }

    /**
     * Asserts that nodes_ are started and connected with each other
     */
    fun assertTopology(vararg nodes_: Int) {

        // Returns the list of nodes that have to be connected to target node
        fun connectedNodes(targetNodeNumber: Int, vararg nodes_: Int): Array<PostchainTestNode> {
            return nodes_
                    .filter { it != targetNodeNumber }
                    .map { nodes[it] }
                    .toTypedArray()
        }

        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    nodes_.forEach {
                        nodes[it].assertNodeConnectedWith(DEFAULT_CHAIN_IID, *connectedNodes(it, *nodes_))
                    }
                }
    }

}