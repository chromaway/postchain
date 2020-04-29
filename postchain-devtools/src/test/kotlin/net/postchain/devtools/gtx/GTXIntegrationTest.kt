// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.gtx

import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.toHex
import net.postchain.core.Transaction
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtx.GTXDataBuilder
import org.junit.Assert
import org.junit.Test

val myCS = SECP256K1CryptoSystem()

class GTXIntegrationTest : IntegrationTestSetup() {

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
        // Need to add a valid dummy operation to make the entire TX valid
        b.addOperation("gtx_test", arrayOf(gtv(1), gtv("true")))
        b.finish()
        b.sign(myCS.buildSigMaker(pubKey(0), privKey(0)))
        return b.serialize()
    }

    @Test
    fun testBuildBlock() {
        configOverrides.setProperty("infrastructure", "base/test")
        val nodes = createNodes(1, "/net/postchain/devtools/gtx_it/blockchain_config.xml")
        val node = nodes[0]
        val bcRid = systemSetup.blockchainMap[1]!!.rid // Just assume we have chain 1

        fun enqueueTx(data: ByteArray): Transaction? {
            try {
                val tx = node.getBlockchainInstance().getEngine().getConfiguration().getTransactionFactory().decodeTransaction(data)
                node.getBlockchainInstance().getEngine().getTransactionQueue().enqueue(tx)
                return tx
            } catch (e: Exception) {
                logger.error(e) { "Can't enqueue tx" }
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

        // Tx1 valid)
        val validTx1 = enqueueTx(makeTestTx(1, "true", bcRid))!!
        validTxs.add(validTx1)

        // Tx 2 invalid, b/c bad args
        enqueueTx(makeTestTx(2, "false", bcRid))!!

        // Tx 3: Nop (invalid, since need more ops)
        val x = makeNOPGTX(bcRid)
        enqueueTx(x)

        // -------------------------
        // Build it
        // -------------------------
        makeSureBlockIsBuiltCorrectly()

        // Tx 4: time, valid, no stop is ok
        val tx4Time = makeTimeBTx(0, null, bcRid)
        validTxs.add(enqueueTx(tx4Time)!!)

        // Tx 5: time, valid, from beginning of time to now
        val tx5Time = makeTimeBTx(0, System.currentTimeMillis(), bcRid)
        validTxs.add(enqueueTx(tx5Time)!!)

        // TX 6: time, invalid since from bigger than to
        val tx6Time = makeTimeBTx(100, 0, bcRid)
        enqueueTx(tx6Time)

        // TX 7: time, invalid since from is in the future
        val tx7Time = makeTimeBTx(System.currentTimeMillis() + 100, null, bcRid)
        enqueueTx(tx7Time)

        // -------------------------
        // Build it
        // -------------------------
        makeSureBlockIsBuiltCorrectly()

        val value = node.getBlockchainInstance().getEngine().getBlockQueries().query(
                """{"type"="gtx_test_get_value", "txRID"="${validTx1.getRID().toHex()}"}""")
        Assert.assertEquals("\"true\"", value.get())
    }
}