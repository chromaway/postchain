package net.postchain.integrationtest.statemachine

import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.devtools.assertChainStarted
import net.postchain.devtools.enqueueTxsAndAwaitBuiltBlock
import net.postchain.integrationtest.reconfiguration.TxChartHelper
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class InvalidBlockTest : IntegrationTest() {

    @Test
    fun node_receivesIncorrectBlock_then_processesItCorrectly_and_nodesReachConsensus() {
        /**
         * This test is about correct processing of incorrect blocks and txs
         * rather than reaching a consensus.
         *
         * Only node1 recognizes blocks and txs as invalid (see FailableTestBlockchainConfigurationFactory).
         *
         * See
         *  - FailableTestBlockchainConfigurationFactory
         *  - FailableTestBlockchainConfiguration
         *  - FailedTestTransactionFactory
         */
        val nodesCount = 2
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val nodeConfigs = arrayOf(
                "classpath:/net/postchain/statemachine/node0.properties",
                "classpath:/net/postchain/statemachine/node1.properties"
        )

        // Chain config
        val blockchainConfigs = arrayOf(
                "/net/postchain/devtools/statemachine/blockchain_config.xml"
        )

        // Creating and launching nodes
        (0 until nodesCount).forEach { i ->
            createSingleNode(i, nodesCount, nodeConfigs[i], blockchainConfigs[0])
        }

        // Asserting chain 1 is started for all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { it.assertChainStarted() }
                }

        // Building a block 0 with two txs via node 0
        nodes[0].enqueueTxsAndAwaitBuiltBlock(PostchainTestNode.DEFAULT_CHAIN_IID, 0, tx(0), tx(1), tx(2))

        // Asserting chain 1 is started for all nodes
        await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    val chart0 = TxChartHelper.buildTxChart(nodes[0], PostchainTestNode.DEFAULT_CHAIN_IID, 0)
                    val chart1 = TxChartHelper.buildTxChart(nodes[1], PostchainTestNode.DEFAULT_CHAIN_IID, 0)
                    JSONAssert.assertEquals(chart0, chart1, JSONCompareMode.NON_EXTENSIBLE)
                    println(chart1)
                }

        // Building a block 0 with two txs via node 0
        nodes[1].enqueueTxsAndAwaitBuiltBlock(PostchainTestNode.DEFAULT_CHAIN_IID, 1, tx(10), tx(11), tx(12))

        // Asserting chain 1 is started for all nodes
        await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    val chart0 = TxChartHelper.buildTxChart(nodes[0], PostchainTestNode.DEFAULT_CHAIN_IID, 1)
                    val chart1 = TxChartHelper.buildTxChart(nodes[1], PostchainTestNode.DEFAULT_CHAIN_IID, 1)
                    JSONAssert.assertEquals(chart0, chart1, JSONCompareMode.NON_EXTENSIBLE)
                    println(chart1)
                }

    }

    private fun tx(id: Int): TestTransaction = TestTransaction(id)
}