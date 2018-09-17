// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.gtx

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import mu.KLogging
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.configurations.GTXTestModule
import net.postchain.test.EbftIntegrationTest
import net.postchain.test.KeyPairHelper
import net.postchain.test.KeyPairHelper.Companion.privKey
import net.postchain.test.KeyPairHelper.Companion.pubKey
import net.postchain.test.OnDemandBlockBuildingStrategy
import net.postchain.test.PostchainTestNode
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureNanoTime

@RunWith(JUnitParamsRunner::class)
class GTXPerformanceTestNightly : EbftIntegrationTest() {

    companion object : KLogging()

    private fun strategy(node: PostchainTestNode): OnDemandBlockBuildingStrategy {
        return node
                .getBlockchainInstance(chainId)
                .getEngine()
                .getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy
    }

    private fun makeTestTx(id: Long, value: String): ByteArray {
        val b = GTXDataBuilder(net.postchain.test.gtx.testBlockchainRID, arrayOf(pubKey(0)), net.postchain.test.gtx.myCS)
        b.addOperation("gtx_test", arrayOf(gtx(id), gtx(value)))
        b.finish()
        b.sign(net.postchain.test.gtx.myCS.makeSigner(pubKey(0), privKey(0)))
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
        configOverrides.setProperty("blockchain.1.blockstrategy", OnDemandBlockBuildingStrategy::class.qualifiedName)
        configOverrides.setProperty("blockchain.1.configurationfactory", GTXBlockchainConfigurationFactory::class.qualifiedName)
        configOverrides.setProperty("blockchain.1.gtx.modules",
                listOf(GTXTestModule::class.qualifiedName, StandardOpsGTXModule::class.qualifiedName))

        createEbftNodes(nodeCount)

        var txId = 0
        val statusManager = ebftNodes[0].getBlockchainInstance(chainId).statusManager
        for (i in 0 until blockCount) {
            for (tx in 0 until txPerBlock) {
                val txFactory = ebftNodes[statusManager.primaryIndex()]
                        .getBlockchainInstance(chainId)
                        .blockchainConfiguration
                        .getTransactionFactory()

                val tx = makeTestTx(1, (txId++).toString())
                ebftNodes[statusManager.primaryIndex()]
                        .getBlockchainInstance(chainId)
                        .getEngine()
                        .getTransactionQueue()
                        .enqueue(txFactory.decodeTransaction(tx))
            }

            val nanoDelta = measureNanoTime {
                strategy(ebftNodes[statusManager.primaryIndex()]).triggerBlock()
                ebftNodes.forEach { strategy(it).awaitCommitted(i) }
            }

            println("Time elapsed: ${nanoDelta / 1000000} ms")
        }
    }

}