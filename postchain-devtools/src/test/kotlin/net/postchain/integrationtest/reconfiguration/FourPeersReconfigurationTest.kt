package net.postchain.integrationtest.reconfiguration

import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_ID
import net.postchain.devtools.testinfra.TestTransaction
import net.postchain.devtools.testinfra.TestTransactionFactory
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.enqueueTxsAndAwaitBuiltBlock
import net.postchain.integrationtest.query
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test
import kotlin.test.assertNotNull

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

        // Asserting that all nodes have block 0 with expected txs
        await().atMost(Duration.TEN_SECONDS.multiply(2))
                .untilAsserted {
                    val actual = setOf(0, 1)
                    assertk.assert(blockTxsIds(nodes[0], 0)).isEqualTo(actual)
                    assertk.assert(blockTxsIds(nodes[1], 0)).isEqualTo(actual)
                    assertk.assert(blockTxsIds(nodes[2], 0)).isEqualTo(actual)
                    assertk.assert(blockTxsIds(nodes[3], 0)).isEqualTo(actual)
                }

        // Awaiting a reconfiguring at height 2
        awaitReconfiguration(2)

        // Asserting blockchainConfig2 with DummyModule2 is loaded by all nodes
        await().atMost(Duration.TEN_SECONDS)
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

        // Building a block 2 with three txs via node 0
        nodes[0].enqueueTxsAndAwaitBuiltBlock(DEFAULT_CHAIN_ID, 2, tx(2), tx(3), tx(4))

        // Asserting that all nodes have block 2 with expected txs
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    val actual = setOf(2, 3, 4)
                    assertk.assert(blockTxsIds(nodes[0], 2)).isEqualTo(actual)
                    assertk.assert(blockTxsIds(nodes[1], 2)).isEqualTo(actual)
                    assertk.assert(blockTxsIds(nodes[2], 2)).isEqualTo(actual)
                    assertk.assert(blockTxsIds(nodes[3], 2)).isEqualTo(actual)
                }

        // Awaiting a reconfiguring at height 5
        awaitReconfiguration(5)

        // Asserting blockchainConfig3 with DummyModule3 is loaded by all nodes
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

    private fun tx(id: Int): TestTransaction = TestTransaction(id)

    private fun blockTxsIds(node: PostchainTestNode, height: Long): Set<Int> {
        val blockRids = node.query(DEFAULT_CHAIN_ID) { it.getBlockRids(height) }
        assertNotNull(blockRids)

        val txsRids = node.query(DEFAULT_CHAIN_ID) { it.getBlockTransactionRids(blockRids!!) }
        assertNotNull(txsRids)

        val txFactory = TestTransactionFactory()
        return txsRids!!.asSequence().map { txRid ->
            val tx = node.query(DEFAULT_CHAIN_ID) { it.getTransaction(txRid) }
            assertNotNull(tx)

            (txFactory.decodeTransaction(tx!!.getRawData()) as TestTransaction).id
        }.toSet()
    }

}