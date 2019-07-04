package net.postchain.integrationtest.reconfiguration

import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_ID
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.enqueueTxsAndAwaitBuiltBlock
import net.postchain.integrationtest.reconfiguration.TxChartHelper.buildTxChart
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

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
        val blockchainConfig1 = "/net/postchain/reconfiguration/four_peers/modules/blockchain_config_1.xml"
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/reconfiguration/four_peers/modules/blockchain_config_2.xml")
        val blockchainConfig3 = readBlockchainConfig(
                "/net/postchain/reconfiguration/four_peers/modules/blockchain_config_3.xml")

        // Creating all nodes
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig1)
        }


        // Adding chain1's blockchainConfig2 with DummyModule2 at different heights to all nodes
        nodes[0].addConfiguration(DEFAULT_CHAIN_ID, 5, blockchainConfig2)
        nodes[1].addConfiguration(DEFAULT_CHAIN_ID, 6, blockchainConfig2)
        nodes[2].addConfiguration(DEFAULT_CHAIN_ID, 7, blockchainConfig2)
        nodes[3].addConfiguration(DEFAULT_CHAIN_ID, 8, blockchainConfig2)

        // Again: Adding chain1's blockchainConfig3 with DummyModule3 at height 7 to all nodes
        nodes[0].addConfiguration(DEFAULT_CHAIN_ID, 10, blockchainConfig3)
        nodes[1].addConfiguration(DEFAULT_CHAIN_ID, 10, blockchainConfig3)
        nodes[2].addConfiguration(DEFAULT_CHAIN_ID, 10, blockchainConfig3)
        nodes[3].addConfiguration(DEFAULT_CHAIN_ID, 10, blockchainConfig3)


        // Asserting chain 1 is started for all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { it.assertChainStarted() }
                }


        // Asserting blockchainConfig1 with DummyModule1 is loaded y all nodes
        assertk.assert(getModules(0).first()).isInstanceOf(DummyModule1::class)
        assertk.assert(getModules(1).first()).isInstanceOf(DummyModule1::class)
        assertk.assert(getModules(2).first()).isInstanceOf(DummyModule1::class)
        assertk.assert(getModules(3).first()).isInstanceOf(DummyModule1::class)

        // Asserting blockchainConfig2 with DummyModule2 is loaded by all nodes
        await().atMost(Duration.TEN_SECONDS.plus(5))
                .untilAsserted {
                    assertk.assert(getModules(0)).isNotEmpty()
                    assertk.assert(getModules(1)).isNotEmpty()
                    assertk.assert(getModules(2)).isNotEmpty()
                    assertk.assert(getModules(3)).isNotEmpty()

                    assertk.assert(getModules(0).first()).isInstanceOf(DummyModule2::class)
                    assertk.assert(getModules(1).first()).isInstanceOf(DummyModule2::class)
                    assertk.assert(getModules(2).first()).isInstanceOf(DummyModule2::class)
                    assertk.assert(getModules(3).first()).isInstanceOf(DummyModule2::class)
                }

        // Asserting blockchainConfig2 with DummyModule3 is loaded by all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(getModules(0)).isNotEmpty()
                    assertk.assert(getModules(1)).isNotEmpty()
                    assertk.assert(getModules(2)).isNotEmpty()
                    assertk.assert(getModules(3)).isNotEmpty()

                    assertk.assert(getModules(0).first()).isInstanceOf(DummyModule3::class)
                    assertk.assert(getModules(1).first()).isInstanceOf(DummyModule3::class)
                    assertk.assert(getModules(2).first()).isInstanceOf(DummyModule3::class)
                    assertk.assert(getModules(3).first()).isInstanceOf(DummyModule3::class)
                }
    }

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
        val blockchainConfig1 = "/net/postchain/reconfiguration/four_peers/signers/blockchain_config_1.xml"
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/reconfiguration/four_peers/signers/blockchain_config_2.xml")
        val blockchainConfig3 = readBlockchainConfig(
                "/net/postchain/reconfiguration/four_peers/signers/blockchain_config_3.xml")

        // Creating all nodes
        nodeConfigsFilenames.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig1)
        }

        // Adding blockchain configs with DummyModule2, DummyModule3, DummyModule4
        // at height 2, 5, 7 to all nodes
        nodes.forEach { it.addConfiguration(DEFAULT_CHAIN_ID, 2, blockchainConfig2) }
        nodes.forEach { it.addConfiguration(DEFAULT_CHAIN_ID, 5, blockchainConfig3) }

        // Asserting chain 1 is started for all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { it.assertChainStarted() }
                }

        // Building a block 0 with two txs via node 0
        nodes[0].enqueueTxsAndAwaitBuiltBlock(DEFAULT_CHAIN_ID, 0, tx(0), tx(1))

        // Awaiting a reconfiguring at height 2
        awaitReconfiguration(2)

        // Asserting blockchainConfig2 with DummyModule2 is loaded by all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { node ->
                        assertk.assert(getModules(node)).isNotEmpty()
                        assertk.assert(getModules(node).first()).isInstanceOf(DummyModule2::class)
                    }
                }

        // Building a block 2 with three txs via node 0
        nodes[0].enqueueTxsAndAwaitBuiltBlock(DEFAULT_CHAIN_ID, 2, tx(2), tx(3), tx(4))

        // Awaiting a reconfiguring at height 5
        awaitReconfiguration(5)

        // Asserting blockchainConfig3 with DummyModule3 is loaded by all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { node ->
                        assertk.assert(getModules(node)).isNotEmpty()
                        assertk.assert(getModules(node).first()).isInstanceOf(DummyModule3::class)
                    }
                }

        // Asserting equality of tx charts of all nodes
        val chart0 = buildTxChart(nodes[0], DEFAULT_CHAIN_ID)
        JSONAssert.assertEquals(chart0, buildTxChart(nodes[1], DEFAULT_CHAIN_ID), JSONCompareMode.NON_EXTENSIBLE)
        JSONAssert.assertEquals(chart0, buildTxChart(nodes[2], DEFAULT_CHAIN_ID), JSONCompareMode.NON_EXTENSIBLE)
        JSONAssert.assertEquals(chart0, buildTxChart(nodes[3], DEFAULT_CHAIN_ID), JSONCompareMode.NON_EXTENSIBLE)

        // Asserting blocks and txs of chart
        val jsonChar0 = ObjectMapper().readTree(chart0)
        assertk.assert((jsonChar0.at("/blocks") as ArrayNode).size()).isEqualTo(5)

        assertk.assert((jsonChar0.at("/blocks/0/tx") as ArrayNode).size()).isEqualTo(2)
        assertk.assert(jsonChar0.at("/blocks/0/tx/0/id").asInt()).isEqualTo(0)
        assertk.assert(jsonChar0.at("/blocks/0/tx/1/id").asInt()).isEqualTo(1)

        assertk.assert((jsonChar0.at("/blocks/1/tx") as ArrayNode).size()).isEqualTo(0)

        assertk.assert((jsonChar0.at("/blocks/2/tx") as ArrayNode).size()).isEqualTo(3)
        assertk.assert(jsonChar0.at("/blocks/2/tx/0/id").asInt()).isEqualTo(2)
        assertk.assert(jsonChar0.at("/blocks/2/tx/1/id").asInt()).isEqualTo(3)
        assertk.assert(jsonChar0.at("/blocks/2/tx/2/id").asInt()).isEqualTo(4)

        assertk.assert((jsonChar0.at("/blocks/3/tx") as ArrayNode).size()).isEqualTo(0)

        assertk.assert((jsonChar0.at("/blocks/4/tx") as ArrayNode).size()).isEqualTo(0)
    }

}