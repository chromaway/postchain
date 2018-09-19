// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.integrationtest

import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiTx
import net.postchain.common.toHex
import net.postchain.test.EbftIntegrationTest
import net.postchain.test.SingleChainTestNode
import net.postchain.test.testinfra.TestTransaction
import org.junit.Assert
import org.junit.Test

class ThreeTxForwardingTest : EbftIntegrationTest() {

    private fun strategy(node: SingleChainTestNode): ThreeTxStrategy {
        return node
                .getBlockchainInstance()
                .getEngine()
                .getBlockBuildingStrategy() as ThreeTxStrategy
    }

    private fun tx(id: Int): ApiTx {
        return ApiTx(TestTransaction(id).getRawData().toHex())
    }

    private fun apiModel(nodeIndex: Int): Model =
            ebftNodes[nodeIndex].getRestApiModel()

    @Test
    fun testTxNotForwardedIfPrimary() {
        configOverrides.setProperty("blockchain.1.blockstrategy", ThreeTxStrategy::class.java.name)
        createEbftNodes(3)

        apiModel(0).postTransaction(tx(0))
        apiModel(1).postTransaction(tx(1))
        apiModel(2).postTransaction(tx(2))
        strategy(ebftNodes[2]).awaitCommitted(0)

        apiModel(0).postTransaction(tx(3))
        apiModel(1).postTransaction(tx(4))
        apiModel(2).postTransaction(tx(5))
        strategy(ebftNodes[2]).awaitCommitted(1)

        apiModel(0).postTransaction(tx(6))
        apiModel(1).postTransaction(tx(7))
        apiModel(2).postTransaction(tx(8))
        strategy(ebftNodes[2]).awaitCommitted(2)

        val bockQueries = ebftNodes[2].getBlockchainInstance().getEngine().getBlockQueries()
        for (i in 0..2) {
            val blockData = bockQueries.getBlockAtHeight(i.toLong()).get()
            Assert.assertEquals(3, blockData.transactions.size)
        }
    }
}