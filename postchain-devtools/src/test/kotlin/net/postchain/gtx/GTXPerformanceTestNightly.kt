// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import mu.KLogging
import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.configurations.GTXTestModule
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.OnDemandBlockBuildingStrategy
import net.postchain.devtools.PostchainTestNode
import net.postchain.ebft.worker.ValidatorWorker
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.factory.GtxTransactionDataFactory
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureNanoTime

@RunWith(JUnitParamsRunner::class)
class GTXPerformanceTestNightly : IntegrationTestSetup() {

    companion object : KLogging()

    val dummyBcRid = BlockchainRid.buildFromHex( "ABABAABABABABABABABABABABABABABAABABABABABABABABABABABABABAABABA")

    private fun strategy(node: PostchainTestNode): OnDemandBlockBuildingStrategy {
        return node
                .getBlockchainInstance()
                .getEngine()
                .getBlockBuildingStrategy() as OnDemandBlockBuildingStrategy
    }

    private fun makeTestTx(id: Long, value: String, blockchainRid: BlockchainRid): ByteArray {
        val b = GTXDataBuilder(blockchainRid, arrayOf(pubKey(0)), net.postchain.devtools.gtx.myCS)
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
                total += makeTestTx(i.toLong(), "Hello", dummyBcRid).size
            }
        }
        Assert.assertTrue(total > 1000)
        println("Time elapsed: ${nanoDelta / 1000000} ms")
    }

    @Test
    fun parseTxData() {
        val transactions = (0..999).map {
            makeTestTx(it.toLong(), it.toString(), dummyBcRid)
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
            makeTestTx(it.toLong(), it.toString(), dummyBcRid)
        }
        var total = 0
        val module = GTXTestModule()
        val cs = SECP256K1CryptoSystem()
        val txFactory = GTXTransactionFactory(dummyBcRid ,module, cs)
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
            makeTestTx(1, it.toString(), dummyBcRid)
        }
        var total = 0
        val module = GTXTestModule()
        val cs = SECP256K1CryptoSystem()
        val txFactory = GTXTransactionFactory(dummyBcRid ,module, cs)
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
            "1, 1000, 0", "1, 1000, 1",
            "4, 1000, 0", "4, 1000, 1"
    )
    fun runXNodesWithYTxPerBlockBuildOnly(nodeCount: Int, txPerBlock: Int, mode: Int) {
        val blocksCount = 2
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodeCount))
        val nodes = createNodes(nodeCount, "/net/postchain/devtools/performance/blockchain_config_$nodeCount.xml")
        val blockchainRid = systemSetup.blockchainMap[1]!!.rid

        var txId = 0
        val statusManager = (nodes[0].getBlockchainInstance() as ValidatorWorker).statusManager
        for (i in 0 until blocksCount) {
            val txs = (1..txPerBlock).map { makeTestTx(1, (txId++).toString(), blockchainRid) }

            val engine = nodes[statusManager.primaryIndex()]
                    .getBlockchainInstance()
                    .getEngine()
            val txFactory = engine
                    .getConfiguration()
                    .getTransactionFactory()
            val queue = engine.getTransactionQueue()

            val nanoDelta = if (mode == 0) {
                txs.forEach {
                    queue.enqueue(txFactory.decodeTransaction(it)) }
                measureNanoTime {
                    nodes.forEach { strategy(it).buildBlocksUpTo(i.toLong()) }
                    nodes.forEach { strategy(it).awaitCommitted(i) }
                }
            } else {
                measureNanoTime {
                    txs.forEach { queue.enqueue(txFactory.decodeTransaction(it)) }
                    nodes.forEach { strategy(it).buildBlocksUpTo(i.toLong()) }
                    nodes.forEach { strategy(it).awaitCommitted(i) }
                }
            }

            println("Time elapsed: ${nanoDelta / 1000000} ms")
        }
    }

}