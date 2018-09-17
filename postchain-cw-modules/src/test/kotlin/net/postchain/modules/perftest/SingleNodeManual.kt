package net.postchain.modules.perftest

import net.postchain.TestNodeEngine
import net.postchain.api.rest.controller.PostchainModel
import net.postchain.api.rest.controller.RestApi
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.TimeLog
import net.postchain.common.toHex
import net.postchain.configurations.GTXTestModule
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.modules.ft.BaseFTModuleFactory
import net.postchain.test.IntegrationTest
import net.postchain.test.KeyPairHelper.Companion.pubKeyHex
import org.junit.Test

/**
 * This runs a single node server without any consensus code. It just
 * receives transactions on the RestApi and builds blocks as fast as it can
 * As long as there are transactions on queue we will fill up the blocks to
 * maximum 1000 tx per block. Block building will restart immediately after
 * current block is committed.
 *
 * To get up to speed before measurements start we will run for 20 seconds without
 * any measurements. Then the actual test starts.
 *
 * If you want to take a cpu-profile of this node, add this vm parameter:
 *
 * -agentlib:hprof=cpu=samples,interval=10,depth=30
 *
 * A file java.hprof.txt will be created in current directory. Note that
 * the warp-up period will be included in the profile.
 *
 */
class SingleNodeManual : IntegrationTest() {
    val assetID = "TST"

    fun runSingleNode(name: String) {
        configOverrides.setProperty("blockchain.1.configurationfactory", GTXBlockchainConfigurationFactory::class.qualifiedName)
        configOverrides.setProperty("blockchain.1.basestrategy.maxblocktransactions", 1000)
        configOverrides.setProperty("blockchain.1.queuecapacity", 100000)
        val node = createDataLayer(0)
        val model = PostchainModel(node.txQueue, node.blockchainConfiguration.getTransactionFactory(), node.blockQueries)
        val api = RestApi(8383, "")
        api.attachModel(
                (node.blockchainConfiguration as BaseBlockchainConfiguration).blockchainRID.toHex(),
                model)

        // warm up
        val warmupDuration = 20000
        var warmupEndTime = System.currentTimeMillis() + warmupDuration
        while (warmupEndTime > System.currentTimeMillis()) {
            buildBlockAndCommit(node)
        }
        val warmup = txCount(node)
        var warmupHeight = warmup.first
        var warmupTxCount = warmup.second

        // Now actual test
        TimeLog.enable(true)
        val testDuration = 60000
        var endTime = System.currentTimeMillis() + testDuration
        while (endTime > System.currentTimeMillis()) {
            buildBlockAndCommit(node)
        }

        val pair = txCount(node)
        val blockCount = pair.first - warmupHeight
        var txCount = pair.second - warmupTxCount

        println("==================================================")
        println("Ran $name for $testDuration ms:")

        println(TimeLog.toString())

        println("Total blocks: ${blockCount}")
        println("Total transaction count: $txCount")
        println("Avg tx/block: ${txCount / (blockCount)}")
        println("node tps: ${txCount * 1000 / testDuration}")
        println("buildBlock tps: ${txCount * 1000 / TimeLog.getValue("BaseBlockchainEngine.buildBlock().buildBlock")}")
    }

    private fun txCount(node: TestNodeEngine): Pair<Long, Int> {
        val bestHeight = node.blockQueries.getBestHeight().get()
        var txCount = 0
        for (i in 0..bestHeight) {
            val blockRid = node.blockQueries.getBlockRids(i.toLong()).get()[0]
            txCount += node.blockQueries.getBlockTransactionRids(blockRid).get().size
        }
        return Pair(bestHeight, txCount)
    }

    @Test
    fun runSingleFTNode() {
        configOverrides.setProperty("blockchain.1.gtx.modules", BaseFTModuleFactory::class.qualifiedName)
        configOverrides.setProperty("blockchain.1.gtx.ft.assets", assetID)
        configOverrides.setProperty("blockchain.1.gtx.ft.asset.$assetID.issuers", pubKeyHex(0))
        configOverrides.setProperty("blockchain.1.gtx.ft.openRegistration", true)
        runSingleNode("FT")
    }

    @Test
    fun runSingleGtxTestNode() {
        configOverrides.setProperty("blockchain.1.gtx.modules", GTXTestModule::class.qualifiedName)
        runSingleNode("GtxTest")
    }
}