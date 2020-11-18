// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.reconfiguration

import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import net.postchain.devtools.*
import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_IID
import net.postchain.integrationtest.reconfiguration.TxChartHelper.buildTxChart
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Ignore
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.timer

class FourPeersReconfigurationTest : ReconfigurationTest() {

    @Test
    fun reconfigurationAtHeight_isSuccessful() {
        val nodesCount = 4
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/reconfiguration/node0.properties",
                "classpath:/net/postchain/reconfiguration/node1.properties",
                "classpath:/net/postchain/reconfiguration/node2.properties",
                "classpath:/net/postchain/reconfiguration/node3.properties"
        )

        // Chains configs
        val blockchainConfig1 = "/net/postchain/devtools/reconfiguration/four_peers/modules/blockchain_config_1.xml"
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/devtools/reconfiguration/four_peers/modules/blockchain_config_2.xml")
        val blockchainConfig3 = readBlockchainConfig(
                "/net/postchain/devtools/reconfiguration/four_peers/modules/blockchain_config_3.xml")

        // Creating all nodes
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig1)
        }


        // Adding chain1's blockchainConfig2 with DummyModule2 at different heights to all nodes
        nodes[0].addConfiguration(DEFAULT_CHAIN_IID, 5, blockchainConfig2)
        nodes[1].addConfiguration(DEFAULT_CHAIN_IID, 6, blockchainConfig2)
        nodes[2].addConfiguration(DEFAULT_CHAIN_IID, 7, blockchainConfig2)
        nodes[3].addConfiguration(DEFAULT_CHAIN_IID, 8, blockchainConfig2)

        // Again: Adding chain1's blockchainConfig3 with DummyModule3 at height 7 to all nodes
        nodes[0].addConfiguration(DEFAULT_CHAIN_IID, 10, blockchainConfig3)
        nodes[1].addConfiguration(DEFAULT_CHAIN_IID, 10, blockchainConfig3)
        nodes[2].addConfiguration(DEFAULT_CHAIN_IID, 10, blockchainConfig3)
        nodes[3].addConfiguration(DEFAULT_CHAIN_IID, 10, blockchainConfig3)


        // Asserting chain 1 is started for all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { it.assertChainStarted() }
                }


        // Asserting blockchainConfig1 with DummyModule1 is loaded y all nodes
        nodes.forEach { node ->
            assertk.assert(node.getModules()).isNotEmpty()
            assertk.assert(node.getModules().first()).isInstanceOf(DummyModule1::class)
        }

        // Asserting blockchainConfig2 with DummyModule2 is loaded by all nodes
        await().atMost(Duration.TEN_SECONDS.multiply(3))
                .untilAsserted {
                    nodes.forEach { node ->
                        assertk.assert(node.getModules()).isNotEmpty()
                        assertk.assert(node.getModules().first()).isInstanceOf(DummyModule2::class)
                    }
                }

        // Asserting blockchainConfig2 with DummyModule3 is loaded by all nodes
        await().atMost(Duration.TEN_SECONDS.multiply(3))
                .untilAsserted {
                    nodes.forEach { node ->
                        assertk.assert(node.getModules()).isNotEmpty()
                        assertk.assert(node.getModules().first()).isInstanceOf(DummyModule3::class)
                    }
                }
    }

    //Duy.chung's comment:
    // I comment asserting block code because want to get rid of the jackson usage and this test was marked as Ignore.
    // So there is no need to try to re-write asserting code by using gson.
    // In case we'd like to remove @Ingore in the the future for this test case, please implement asserting using gson instead.
    @Ignore
    @Test
    fun reconfigurationAtHeight_whenSignersAreChanged_isSuccessful() {
        val nodesCount = 4
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/reconfiguration/node0.properties",
                "classpath:/net/postchain/reconfiguration/node1.properties",
                "classpath:/net/postchain/reconfiguration/node2.properties",
                "classpath:/net/postchain/reconfiguration/node3.properties"
        )

        // Chains configs
        val blockchainConfig1 = "/net/postchain/devtools/reconfiguration/four_peers/signers/blockchain_config_1.xml"
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/devtools/reconfiguration/four_peers/signers/blockchain_config_2.xml")
        val blockchainConfig3 = readBlockchainConfig(
                "/net/postchain/devtools/reconfiguration/four_peers/signers/blockchain_config_3.xml")

        // Creating all nodes
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig1)
        }

        // Adding blockchain configs with DummyModule2, DummyModule3, DummyModule4
        // at height 2, 5, 7 to all nodes
        nodes.forEach { it.addConfiguration(DEFAULT_CHAIN_IID, 2, blockchainConfig2) }
        nodes.forEach { it.addConfiguration(DEFAULT_CHAIN_IID, 5, blockchainConfig3) }

        // Asserting chain 1 is started for all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { it.assertChainStarted() }
                }

        // Building a block 0 with two txs via node 0
        nodes[0].enqueueTxsAndAwaitBuiltBlock(DEFAULT_CHAIN_IID, 0, tx(0), tx(1))

        // Awaiting a reconfiguring at height 2
        awaitReconfiguration(2)

        // Asserting blockchainConfig2 with DummyModule2 is loaded by all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { node ->
                        assertk.assert(node.getModules()).isNotEmpty()
                        assertk.assert(node.getModules().first()).isInstanceOf(DummyModule2::class)
                    }
                }

        // Building a block 2 with three txs via node 0
        nodes[0].enqueueTxsAndAwaitBuiltBlock(DEFAULT_CHAIN_IID, 2, tx(2), tx(3), tx(4))

        // Awaiting a reconfiguring at height 5
        awaitReconfiguration(5)

        // Asserting blockchainConfig3 with DummyModule3 is loaded by all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { node ->
                        assertk.assert(node.getModules()).isNotEmpty()
                        assertk.assert(node.getModules().first()).isInstanceOf(DummyModule3::class)
                    }
                }

        // Asserting equality of tx charts of all nodes
        val chart0 = buildTxChart(nodes[0], DEFAULT_CHAIN_IID)
        JSONAssert.assertEquals(chart0, buildTxChart(nodes[1], DEFAULT_CHAIN_IID), JSONCompareMode.NON_EXTENSIBLE)
        JSONAssert.assertEquals(chart0, buildTxChart(nodes[2], DEFAULT_CHAIN_IID), JSONCompareMode.NON_EXTENSIBLE)
        JSONAssert.assertEquals(chart0, buildTxChart(nodes[3], DEFAULT_CHAIN_IID), JSONCompareMode.NON_EXTENSIBLE)

        // Asserting blocks and txs of chart
//        val jsonChar0 = gson.fromJson(chart0, JsonElement::class.java).asJsonObject
//        assertk.assert((jsonChar0.at("/blocks") as ArrayNode).size()).isEqualTo(5)

//        assertk.assert((jsonChar0.at("/blocks/0/tx") as ArrayNode).size()).isEqualTo(2)
//        assertk.assert(jsonChar0.at("/blocks/0/tx/0/id").asInt()).isEqualTo(0)
//        assertk.assert(jsonChar0.at("/blocks/0/tx/1/id").asInt()).isEqualTo(1)
//
//        assertk.assert((jsonChar0.at("/blocks/1/tx") as ArrayNode).size()).isEqualTo(0)
//
//        assertk.assert((jsonChar0.at("/blocks/2/tx") as ArrayNode).size()).isEqualTo(3)
//        assertk.assert(jsonChar0.at("/blocks/2/tx/0/id").asInt()).isEqualTo(2)
//        assertk.assert(jsonChar0.at("/blocks/2/tx/1/id").asInt()).isEqualTo(3)
//        assertk.assert(jsonChar0.at("/blocks/2/tx/2/id").asInt()).isEqualTo(4)
//
//        assertk.assert((jsonChar0.at("/blocks/3/tx") as ArrayNode).size()).isEqualTo(0)
//
//        assertk.assert((jsonChar0.at("/blocks/4/tx") as ArrayNode).size()).isEqualTo(0)
    }

    // TODO: Olle fix for POS-76
    @Ignore
    @Test
    fun reconfigurationAtHeight_withBaseBlockBuildingStrategy_withManyTxs_whenSignersAreChanged_isSuccessful() {
        val nodesCount = 4
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/reconfiguration/node0.properties",
                "classpath:/net/postchain/reconfiguration/node1.properties",
                "classpath:/net/postchain/reconfiguration/node2.properties",
                "classpath:/net/postchain/reconfiguration/node3.properties"
        )

        // Chains configs
        val blockchainConfig1 = "/net/postchain/devtools/reconfiguration/four_peers/signers2/blockchain_config_1.xml"
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/devtools/reconfiguration/four_peers/signers2/blockchain_config_2.xml")
        val blockchainConfig3 = readBlockchainConfig(
                "/net/postchain/devtools/reconfiguration/four_peers/signers2/blockchain_config_3.xml")
        val blockchainConfig4 = readBlockchainConfig(
                "/net/postchain/devtools/reconfiguration/four_peers/signers2/blockchain_config_4.xml")

        // Creating all nodes
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig1)
        }

        // Adding blockchain configs with DummyModule2, DummyModule3, DummyModule4
        // at height 2, 5, 7 to all nodes
        nodes.forEach { it.addConfiguration(DEFAULT_CHAIN_IID, 2, blockchainConfig2) }
        nodes.forEach { it.addConfiguration(DEFAULT_CHAIN_IID, 5, blockchainConfig3) }
        nodes.forEach { it.addConfiguration(DEFAULT_CHAIN_IID, 8, blockchainConfig4) }

        // Asserting chain 1 is started for all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { it.assertChainStarted() }
                }

        // Enqueueing txs
        val latch = CountDownLatch(1)
        var txId = 0
        val txTimer = timer(name = "txTimer", period = 500) {
            if (txId < 100) {
                // Enqueueing txs via node0 and node1
                if (nodes[txId % 2].enqueueTxs(DEFAULT_CHAIN_IID, tx(txId))) {
                    ++txId
                }
            } else {
                latch.countDown()
            }
        }

        // Waiting for all txs to be enqueued
        latch.await()

        txTimer.cancel()
        txTimer.purge()

        // Asserting equality of `tx charts of all nodes
        await().atMost(Duration.TEN_SECONDS.multiply(4))
                .untilAsserted {
                    // Asserting equality of tx charts of all nodes

                    // Building tx charts of the minimum (common) length for all nodes
                    val commonHeight = nodes.map { node ->
                        node.query(DEFAULT_CHAIN_IID) { it.getBestHeight() } ?: -1L
                    }.min() ?: -1L

                    assertk.assert(commonHeight > 0L).isTrue()

                    val chart0 = buildTxChart(nodes[0], DEFAULT_CHAIN_IID, commonHeight)
                    val chart1 = buildTxChart(nodes[1], DEFAULT_CHAIN_IID, commonHeight)
                    val chart2 = buildTxChart(nodes[2], DEFAULT_CHAIN_IID, commonHeight)
                    val chart3 = buildTxChart(nodes[3], DEFAULT_CHAIN_IID, commonHeight)

                    JSONAssert.assertEquals(chart0, chart1, JSONCompareMode.NON_EXTENSIBLE)
                    JSONAssert.assertEquals(chart0, chart2, JSONCompareMode.NON_EXTENSIBLE)
                    JSONAssert.assertEquals(chart0, chart3, JSONCompareMode.NON_EXTENSIBLE)
                }


        /* TODO: [et]: We lose some txs during reconfiguration. Will be fixed later.
        // Asserting equality of tx charts of all nodes
        await().atMost(Duration.TEN_SECONDS.multiply(2))
                .untilAsserted {
                    // Asserting all txs
                    val txs = collectAllTxs(nodes[0], DEFAULT_CHAIN_IID)
                            .asSequence()
                            .map { (it as TestTransaction).id }
                            .toSet()

                    assertk.assert(txs).hasSize(100)
                    assertk.assert(txs).containsAll(*(0 until 100).toList().toTypedArray())
                }
                */
    }

}