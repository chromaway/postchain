// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.gtx

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import mu.KLogging
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.configurations.GTXTestModule
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.SingleChainTestNode
import net.postchain.gtv.GtvFactory.gtv
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureNanoTime

@RunWith(JUnitParamsRunner::class)
class GTXPerformanceTestNightly : IntegrationTest() {

    companion object : KLogging()

    private fun strategy(node: SingleChainTestNode): OnDemandBlockBuildingStrategy {
        return node
                .getBlockchainInstance()
                .getEngine()
                .getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy
    }

    private fun makeTestTx(id: Long, value: String): ByteArray {
        val b = GTXDataBuilder(net.postchain.devtools.gtx.testBlockchainRID, arrayOf(pubKey(0)), net.postchain.devtools.gtx.myCS)
        b.addOperation("gtx_test", arrayOf(gtv(id), gtv(value)))
        b.finish()
        b.sign(net.postchain.devtools.gtx.myCS.makeSigner(pubKey(0), privKey(0)))
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
            for (tx in transactions) {
                total += decodeGTXData(tx).operations.size
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
        val nanoDelta = measureNanoTime {
            for (tx in transactions) {
                val ttx = GTXTransaction(tx, module, cs)
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
        val nanoDelta = measureNanoTime {
            for (tx in transactions) {
                val ttx = GTXTransaction(tx, module, cs)
                total += ttx.ops.size
                Assert.assertTrue(ttx.isCorrect())
            }
        }
        Assert.assertTrue(total == 1000)
        println("Time elapsed: ${nanoDelta / 1000000} ms")
    }

    @Test
    @Parameters(
            "3, 100", "4, 100", "10, 100",
            "4, 1000", "10, 1000"
            //"4, 10", "10, 10", "16, 10"
    )
    fun runXNodesWithYTxPerBlock(nodeCount: Int, txPerBlock: Int) {
        val blockCount = 2
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodeCount))
        createNodes(nodeCount, "/net/postchain/performance/blockchain_config_$nodeCount.xml")

        var txId = 0
        val statusManager = nodes[0].getBlockchainInstance().statusManager
        for (i in 0 until blockCount) {
            for (tx in 0 until txPerBlock) {
                val txFactory = nodes[statusManager.primaryIndex()]
                        .getBlockchainInstance()
                        .blockchainConfiguration
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