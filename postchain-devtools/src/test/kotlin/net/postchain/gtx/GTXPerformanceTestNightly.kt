// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.gtx

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import mu.KLogging
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.hexStringToByteArray
import net.postchain.configurations.GTXTestModule
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.factory.GtxTransactionDataFactory
import net.postchain.devtools.PostchainTestNode
import net.postchain.ebft.worker.ValidatorWorker
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureNanoTime

@RunWith(JUnitParamsRunner::class)
class GTXPerformanceTestNightly : IntegrationTest() {

    companion object : KLogging()

    val expectedBcRid = "78967BAA4768CBCEF11C508326FFB13A956689FCB6DC3BA17F4B895CBB1577A3".hexStringToByteArray()

    private fun strategy(node: PostchainTestNode): OnDemandBlockBuildingStrategy {
        return node
                .getBlockchainInstance()
                .getEngine()
                .getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy
    }

    private fun makeTestTx(id: Long, value: String): ByteArray {
        val b = GTXDataBuilder(net.postchain.devtools.gtx.testBlockchainRID, arrayOf(pubKey(0)), net.postchain.devtools.gtx.myCS)
        b.addOperation("gtx_test", arrayOf(gtv(id), gtv(value)))
        b.finish()
        b.sign(net.postchain.devtools.gtx.myCS.buildSigMaker(pubKey(0), privKey(0)))
        return b.serialize()
    }

    @Test
    fun makeTx() {
        var total = 0
        val nanoDelta = measureNanoTime {
            for (i in 0..999) {
                total += makeTestTx(i.toLong(), "Hello").size
            }
        }
        Assert.assertTrue(total > 1000)
        println("Time elapsed: ${nanoDelta / 1000000} ms")
    }

    @Test
    fun parseTxData() {
        val transactions = (0..999).map {
            makeTestTx(it.toLong(), it.toString())
        }
        var total = 0
        val nanoDelta = measureNanoTime {
            for (rawTx in transactions) {
                val gtvData = GtvFactory.decodeGtv(rawTx)
                val gtxData = GtxTransactionDataFactory.deserializeFromGtv(gtvData)
                total += gtxData.transactionBodyData.operations.size
            }
        }
        Assert.assertTrue(total == 1000)
        println("Time elapsed: ${nanoDelta / 1000000} ms")
    }

    @Test
    fun parseTx() {
        val transactions = (0..999).map {
            makeTestTx(it.toLong(), it.toString())
        }
        var total = 0
        val module = GTXTestModule()
        val cs = SECP256K1CryptoSystem()
        val txFactory = GTXTransactionFactory(expectedBcRid ,module, cs)
        val nanoDelta = measureNanoTime {
            for (rawTx in transactions) {
                val ttx =  txFactory.decodeTransaction(rawTx) as GTXTransaction
                total += ttx.ops.size
            }
        }
        Assert.assertTrue(total == 1000)
        println("Time elapsed: ${nanoDelta / 1000000} ms")
    }

    @Test
    fun parseTxVerify() {
        val transactions = (0..999).map {
            makeTestTx(1, it.toString())
        }
        var total = 0
        val module = GTXTestModule()
        val cs = SECP256K1CryptoSystem()
        val txFactory = GTXTransactionFactory(expectedBcRid ,module, cs)
        val nanoDelta = measureNanoTime {
            for (rawTx in transactions) {
                val ttx =  txFactory.decodeTransaction(rawTx) as GTXTransaction
                total += ttx.ops.size
                Assert.assertTrue(ttx.isCorrect())
            }
        }
        Assert.assertTrue(total == 1000)
        println("Time elapsed: ${nanoDelta / 1000000} ms")
    }

    @Test
    @Parameters(
            "1, 100", "1, 1000"
/*            "3, 100", "4, 100", "10, 100",
            "4, 1000"*/
    )
    fun runXNodesWithYTxPerBlock(nodeCount: Int, txPerBlock: Int) {
        val blocksCount = 2
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodeCount))
        createNodes(nodeCount, "/net/postchain/devtools/performance/blockchain_config_$nodeCount.xml")

        var txId = 0
        val statusManager = (nodes[0].getBlockchainInstance() as ValidatorWorker).statusManager
        for (i in 0 until blocksCount) {
            for (tx in 0 until txPerBlock) {
                val txFactory = nodes[statusManager.primaryIndex()]
                        .getBlockchainInstance()
                        .getEngine()
                        .getConfiguration()
                        .getTransactionFactory()

                val tx = makeTestTx(1, (txId++).toString())
                nodes[statusManager.primaryIndex()]
                        .getBlockchainInstance()
                        .getEngine()
                        .getTransactionQueue()
                        .enqueue(txFactory.decodeTransaction(tx))
            }

            val nanoDelta = measureNanoTime {
                nodes.forEach { strategy(it).buildBlocksUpTo(i.toLong()) }
                nodes.forEach { strategy(it).awaitCommitted(i) }
            }

            println("Time elapsed: ${nanoDelta / 1000000} ms")
        }
    }

}