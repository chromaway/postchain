// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.modules.ft

import net.postchain.common.toHex
import net.postchain.core.Transaction
import net.postchain.devtools.modules.ft.FTIntegrationTest
import net.postchain.gtv.Gtv
import net.postchain.gtv.make_gtv_gson
import org.junit.Assert
import org.junit.Test

class FTBasicIntegrationTest : FTIntegrationTest() {

    @Test
    fun testEverything() {
        configOverrides.setProperty("infrastructure", "base/test")
        val nodes = createNodes(1, "/net/postchain/ft_basic/blockchain_config.xml")
        val node = nodes[0]
        val validTxs = mutableListOf<Transaction>()
        var currentBlockHeight = -1L
        val bcRid = node.getBlockchainRid(1L)!!
        setBlockchainRid(bcRid) // Yes it's ugly but this is old stuff

        fun makeSureBlockIsBuiltCorrectly() {
            currentBlockHeight += 1
            buildBlockAndCommit(node)
            Assert.assertEquals(currentBlockHeight, getBestHeight(node))
            val ridsAtHeight = getTxRidsAtHeight(node, currentBlockHeight)
            for (vtx in validTxs) {
                val vtxRID = vtx.getRID()
                Assert.assertTrue(ridsAtHeight.any { it.contentEquals(vtxRID) })
            }
            Assert.assertEquals(validTxs.size, ridsAtHeight.size)
            validTxs.clear()
        }

        validTxs.add(enqueueTx(
                node,
                makeRegisterTx(arrayOf(aliceAccountDesc, bobAccountDesc), 0)
        )!!)

        makeSureBlockIsBuiltCorrectly()

        validTxs.add(enqueueTx(
                node,
                makeIssueTx(0, issuerID, aliceAccountID, "USD", 1000)
        )!!)

        // invalid issuance:
        enqueueTx(node, makeIssueTx(0, issuerID, aliceAccountID, "XDX", 1000))
        enqueueTx(node, makeIssueTx(0, issuerID, aliceAccountID, "USD", -1000))
        enqueueTx(node, makeIssueTx(0, aliceAccountID, aliceAccountID, "USD", 1000))
        enqueueTx(node, makeIssueTx(1, issuerID, aliceAccountID, "USD", 1000))
        enqueueTx(node, makeIssueTx(0, issuerID, invalidAccountID, "USD", 1000))

        makeSureBlockIsBuiltCorrectly()

        validTxs.add(enqueueTx(
                node,
                makeTransferTx(1, aliceAccountID, "USD", 100, bobAccountID)
        )!!)

        enqueueTx(node, makeTransferTx(1, aliceAccountID, "USD", 10000, bobAccountID))
        enqueueTx(node, makeTransferTx(1, aliceAccountID, "USD", -100, bobAccountID))
        enqueueTx(node, makeTransferTx(2, aliceAccountID, "USD", 100, bobAccountID))

        makeSureBlockIsBuiltCorrectly()

        validTxs.add(enqueueTx(
                node,
                makeTransferTx(1, aliceAccountID, "USD", 1, bobAccountID, "hi")
        )!!)
        validTxs.add(enqueueTx(
                node,
                makeTransferTx(1, aliceAccountID, "USD", 1, bobAccountID, null, "there")
        )!!)
        validTxs.add(enqueueTx(
                node,
                makeTransferTx(1, aliceAccountID, "USD", 1, bobAccountID, "hi", "there")
        )!!)

        makeSureBlockIsBuiltCorrectly()

        val blockQueries = node.getBlockchainInstance().getEngine().getBlockQueries()
        val balance = blockQueries.query(
                """{"type"="ft_get_balance",
                    "account_id"="${aliceAccountID.toHex()}",
                    "asset_id"="USD"
                   }""")
        Assert.assertEquals("""{"balance":897}""", balance.get())
        val existence = blockQueries.query(
                """{"type"="ft_account_exists",
                    "account_id"="${invalidAccountID.toHex()}"
                   }""")
        Assert.assertEquals("""{"exists":0}""", existence.get())
        val history = blockQueries.query(
                """{"type"="ft_get_history",
                    "account_id"="${aliceAccountID.toHex()}",
                    "asset_id"="USD"
                   }""").get()
        println(history)
        val gson = make_gtv_gson()
        val historyGTX = gson.fromJson<Gtv>(history, Gtv::class.java)
        Assert.assertEquals(5, historyGTX.asArray().size)

        val history2 = blockQueries.query(
                """{"type"="ft_get_history",
                    "account_id"="${bobAccountID.toHex()}",
                    "asset_id"="USD"
                   }""").get()
        println(history2)
        val history2GTX = gson.fromJson<Gtv>(history2, Gtv::class.java)
        Assert.assertEquals(4, history2GTX.asArray().size)
    }
}