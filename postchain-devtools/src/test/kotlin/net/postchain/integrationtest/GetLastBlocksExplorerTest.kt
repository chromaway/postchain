package net.postchain.integrationtest

import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiTx
import net.postchain.common.toHex
import net.postchain.core.BlockDetail
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.testinfra.TestTransaction
import org.junit.Assert
import org.junit.Test

class GetLastBlocksExplorerTest : IntegrationTest() {

    private fun tx(id: Int): ApiTx {
        return ApiTx(TestTransaction(id).getRawData().toHex())
    }

    private fun apiModel(nodeIndex: Int): Model = nodes[nodeIndex].getRestApiModel()

    @Test
    fun buildOneBlock() {
        val count = 2
        configOverrides.setProperty("testpeerinfos", createPeerInfos(count))
        val nodeConfig0 = "classpath:/net/postchain/rest_api/node0.properties"
        val nodeConfig1 = "classpath:/net/postchain/rest_api/node1.properties"
        val blockchainConfig = "/net/postchain/three_tx/blockchain_config.xml"

        createSingleNode(0, 2, nodeConfig0, blockchainConfig)
        createSingleNode(1, 2, nodeConfig1, blockchainConfig)

        apiModel(0).postTransaction(tx(0))

        enqueueTx(nodes[0], tx(1).bytes, -1)
        enqueueTx(nodes[1], tx(1).bytes, -1)
        buildBlockAndCommit(nodes[0])
        buildBlockAndCommit(nodes[1])

        buildBlockAndCommit(nodes[0])
        buildBlockAndCommit(nodes[1])

        enqueueTx(nodes[1], tx(2).bytes, 1)
        enqueueTx(nodes[0], tx(2).bytes, 1)
        buildBlockAndCommit(nodes[1])
        buildBlockAndCommit(nodes[0])

        val query0 = nodes[0].getRestApiModel().getLatestBlocksUpTo(Long.MAX_VALUE, 25)
        val query1 = nodes[1].getRestApiModel().getLatestBlocksUpTo(Long.MAX_VALUE, 25)

        (0 until query0.size).forEach {
            Assert.assertTrue(
                    compareBlocks(query0[it], query1[it]))
        }
    }

    private fun compareBlocks(block0: BlockDetail, block1: BlockDetail): Boolean {
        if ((block0.height != block1.height) ||
                (block0.transactions.size != block1.transactions.size)) return false

        val thisTransactionsToHex = block0.transactions.map { transaction -> transaction.toHex() }
        for (transaction in block1.transactions) {
            if (!thisTransactionsToHex.contains(transaction.toHex())) return false
        }

        return true
    }
}