// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.test.gtx

import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.configurations.GTXTestModule
import net.postchain.core.Transaction
import net.postchain.gtx.*
import net.postchain.test.IntegrationTest
import net.postchain.test.KeyPairHelper.privKey
import net.postchain.test.KeyPairHelper.pubKey
import org.junit.Assert
import org.junit.Test

val testBlockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3".hexStringToByteArray()
val myCS = SECP256K1CryptoSystem()


class GTXIntegrationTest : IntegrationTest() {

    fun makeNOPGTX(): ByteArray {
        val b = GTXDataBuilder(testBlockchainRID, arrayOf(pubKey(0)), myCS)
        b.addOperation("nop", arrayOf(gtx(42)))
        b.finish()
        b.sign(myCS.makeSigner(pubKey(0), privKey(0)))
        return b.serialize()
    }

    fun makeTestTx(id: Long, value: String): ByteArray {
        val b = GTXDataBuilder(testBlockchainRID, arrayOf(pubKey(0)), myCS)
        b.addOperation("gtx_test", arrayOf(gtx(id), gtx(value)))
        b.finish()
        b.sign(myCS.makeSigner(pubKey(0), privKey(0)))
        return b.serialize()
    }

    fun makeTimeBTx(from: Long, to: Long?): ByteArray {
        val b = GTXDataBuilder(testBlockchainRID, arrayOf(pubKey(0)), myCS)
        b.addOperation("timeb", arrayOf(
                gtx(from),
                if (to != null) gtx(to) else GTXNull
        ))
        b.finish()
        b.sign(myCS.makeSigner(pubKey(0), privKey(0)))
        return b.serialize()
    }

    @Test
    fun testBuildBlock() {
        configOverrides.setProperty("blockchain.1.configurationfactory", GTXBlockchainConfigurationFactory::class.qualifiedName)
        configOverrides.setProperty("blockchain.1.gtx.modules",
                listOf(GTXTestModule::class.qualifiedName, StandardOpsGTXModule::class.qualifiedName))
        val (node, chainId) = createNode(0)

        fun enqueueTx(data: ByteArray): Transaction? {
            try {
                val tx = node.getBlockchainInstance(chainId).blockchainConfiguration.getTransactionFactory().decodeTransaction(data)
                node.getBlockchainInstance(chainId).getEngine().getTransactionQueue().enqueue(tx)
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
            buildBlockAndCommit(node.getBlockchainInstance(chainId).getEngine())
            Assert.assertEquals(currentBlockHeight, getBestHeight(node, chainId))
            val ridsAtHeight = getTxRidsAtHeight(node, chainId, currentBlockHeight)
            for (vtx in validTxs) {
                val vtxRID = vtx.getRID()
                Assert.assertTrue(ridsAtHeight.any { it.contentEquals(vtxRID) })
            }
            Assert.assertEquals(validTxs.size, ridsAtHeight.size)
            validTxs.clear()
        }

        val validTx1 = enqueueTx(makeTestTx(1, "true"))!!
        validTxs.add(validTx1)
        enqueueTx(makeTestTx(2, "false"))
        validTxs.add(enqueueTx(makeNOPGTX())!!)

        makeSureBlockIsBuiltCorrectly()

        validTxs.add(enqueueTx(makeTimeBTx(0, null))!!)
        validTxs.add(enqueueTx(makeTimeBTx(0, System.currentTimeMillis()))!!)

        enqueueTx(makeTimeBTx(100, 0))
        enqueueTx(makeTimeBTx(System.currentTimeMillis() + 100, null))

        makeSureBlockIsBuiltCorrectly()

        val value = node.getBlockchainInstance(chainId).getEngine().getBlockQueries().query(
                """{"type"="gtx_test_get_value", "txRID"="${validTx1.getRID().toHex()}"}""")
        Assert.assertEquals("\"true\"", value.get())
    }
}