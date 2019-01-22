package net.postchain.modules.perftest

import net.postchain.common.TimeLog
import net.postchain.configurations.GTXTestModule
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.modules.ft.BaseFTModuleFactory
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.KeyPairHelper.pubKeyHex
import net.postchain.devtools.SingleChainTestNode
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

        val node = createNode(0)

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

    private fun txCount(node: SingleChainTestNode): Pair<Long, Int> {
        return node.getBlockchainInstance().getEngine().getBlockQueries().let { blockQueries ->
            val bestHeight = blockQueries.getBestHeight().get()
            var txCount = (0..bestHeight).fold(0) { count, height ->
                val blockRid = blockQueries.getBlockRids(height).get()[0]
                count + blockQueries.getBlockTransactionRids(blockRid).get().size
            }

            Pair(bestHeight, txCount)
        }
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