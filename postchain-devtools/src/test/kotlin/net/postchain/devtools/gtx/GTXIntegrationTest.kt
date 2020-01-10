// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.devtools.gtx

import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.Transaction
import net.postchain.gtx.*
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.gtv.GtvNull
import net.postchain.gtx.GTXDataBuilder
import org.junit.Assert
import org.junit.Test

val myCS = SECP256K1CryptoSystem()

class GTXIntegrationTest : IntegrationTest() {

    fun makeNOPGTX(bcRid: BlockchainRid): ByteArray {
        val b = GTXDataBuilder(bcRid, arrayOf(pubKey(0)), myCS)
        b.addOperation("nop", arrayOf(gtv(42)))
        b.finish()
        b.sign(myCS.buildSigMaker(pubKey(0), privKey(0)))
        return b.serialize()
    }

    fun makeTestTx(id: Long, value: String, bcRid: BlockchainRid): ByteArray {
        val b = GTXDataBuilder(bcRid, arrayOf(pubKey(0)), myCS)
        b.addOperation("gtx_test", arrayOf(gtv(id), gtv(value)))
        b.finish()
        b.sign(myCS.buildSigMaker(pubKey(0), privKey(0)))
        return b.serialize()
    }

    fun makeTimeBTx(from: Long, to: Long?, bcRid: BlockchainRid): ByteArray {
        val b = GTXDataBuilder(bcRid, arrayOf(pubKey(0)), myCS)
        b.addOperation("timeb", arrayOf(
                gtv(from),
                if (to != null) gtv(to) else GtvNull
        ))
        b.finish()
        b.sign(myCS.buildSigMaker(pubKey(0), privKey(0)))
        return b.serialize()
    }

    @Test
    fun testBuildBlock() {
        configOverrides.setProperty("infrastructure", "base/test")
        val node = createNode(0, "/net/postchain/devtools/gtx_it/blockchain_config.xml")
        val bcRid = node.getBlockchainRid(1L)!! // Just assume we have chain 1

        fun enqueueTx(data: ByteArray): Transaction? {
            try {
                val tx = node.getBlockchainInstance().getEngine().getConfiguration().getTransactionFactory().decodeTransaction(data)
                node.getBlockchainInstance().getEngine().getTransactionQueue().enqueue(tx)
                return tx
            } catch (e: Exception) {
                println(e)
            }
            return null
        }

        val validTxs = mutableListOf<Transaction>()
        var currentBlockHeight = -1L

        fun makeSureBlockIsBuiltCorrectly() {
            currentBlockHeight += 1
            buildBlockAndCommit(node.getBlockchainInstance().getEngine())
            Assert.assertEquals(currentBlockHeight, getBestHeight(node))
            val ridsAtHeight = getTxRidsAtHeight(node, currentBlockHeight)
            for (vtx in validTxs) {
                val vtxRID = vtx.getRID()
                Assert.assertTrue(ridsAtHeight.any { it.contentEquals(vtxRID) })
            }
            Assert.assertEquals(validTxs.size, ridsAtHeight.size)
            validTxs.clear()
        }

        val validTx1 = enqueueTx(makeTestTx(1, "true", bcRid))!!
        validTxs.add(validTx1)
        enqueueTx(makeTestTx(2, "false", bcRid))
        val x =makeNOPGTX(bcRid)
        validTxs.add(enqueueTx(x)!!)

        makeSureBlockIsBuiltCorrectly()

        validTxs.add(enqueueTx(makeTimeBTx(0, null, bcRid))!!)
        validTxs.add(enqueueTx(makeTimeBTx(0, System.currentTimeMillis(), bcRid))!!)

        enqueueTx(makeTimeBTx(100, 0, bcRid))
        enqueueTx(makeTimeBTx(System.currentTimeMillis() + 100, null, bcRid))

        makeSureBlockIsBuiltCorrectly()

        val value = node.getBlockchainInstance().getEngine().getBlockQueries().query(
                """{"type"="gtx_test_get_value", "txRID"="${validTx1.getRID().toHex()}"}""")
        Assert.assertEquals("\"true\"", value.get())
    }
}