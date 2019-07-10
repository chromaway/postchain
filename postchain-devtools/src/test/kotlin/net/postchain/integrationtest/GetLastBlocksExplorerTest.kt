package net.postchain.integrationtest

import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import net.postchain.common.toHex
import net.postchain.core.Transaction
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_ID
import net.postchain.devtools.testinfra.TestTransaction
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test

class GetLastBlocksExplorerTest : IntegrationTest() {

    @Test
    fun buildOneBlock() {
        val nodesCount = 1
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val nodeConfig = "classpath:/net/postchain/rest_api/node0.properties"
        val blockchainConfig = "/net/postchain/blockexplorer/blockchain_config.xml"

        // Creating all nodes
        createSingleNode(0, nodesCount, nodeConfig, blockchainConfig)

        // Asserting chain 1 is started for all nodes
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes.forEach { it.assertChainStarted() }
                }

        nodes[0].enqueueTxsAndAwaitBuiltBlock(DEFAULT_CHAIN_ID, 0, tx(0), tx(1))
        nodes[0].enqueueTxsAndAwaitBuiltBlock(DEFAULT_CHAIN_ID, 1, tx(10), tx(11), tx(12))
        nodes[0].enqueueTxsAndAwaitBuiltBlock(DEFAULT_CHAIN_ID, 2, tx(100), tx(101), tx(102))

        // Asserting blocks and txs
        val blocks = nodes[0].getRestApiModel().getLatestBlocksUpTo(Long.MAX_VALUE, 25)
        assertk.assert(blocks).hasSize(3)

        // Block #2
        assertk.assert(blocks[0].height).isEqualTo(2L)
        assertk.assert(blocks[0].transactions).hasSize(3)
        assertk.assert(compareTx(blocks[0].transactions[0], tx(100))).isTrue()
        assertk.assert(compareTx(blocks[0].transactions[1], tx(101))).isTrue()
        assertk.assert(compareTx(blocks[0].transactions[2], tx(102))).isTrue()

        // Block #1
        assertk.assert(blocks[1].height).isEqualTo(1L)
        assertk.assert(blocks[1].transactions).hasSize(3)
        assertk.assert(compareTx(blocks[1].transactions[0], tx(10))).isTrue()
        assertk.assert(compareTx(blocks[1].transactions[1], tx(11))).isTrue()
        assertk.assert(compareTx(blocks[1].transactions[2], tx(12))).isTrue()

        // Block #0
        assertk.assert(blocks[2].height).isEqualTo(0L)
        assertk.assert(blocks[2].transactions).hasSize(2)
        assertk.assert(compareTx(blocks[2].transactions[0], tx(0))).isTrue()
        assertk.assert(compareTx(blocks[2].transactions[1], tx(1))).isTrue()
    }

    private fun tx(id: Int): TestTransaction = TestTransaction(id)

    private fun compareTx(actualTx: ByteArray, expectedTx: Transaction): Boolean {
        return (actualTx.toHex() == expectedTx.getRawData().toHex())
                .also {
                    if (!it) {
                        logger.error {
                            "Transactions are not equal:\n" +
                                    "\t actual:\t${actualTx.toHex()}\n" +
                                    "\t expected:\t${expectedTx.getRawData().toHex()}"
                        }
                    }
                }
    }
}