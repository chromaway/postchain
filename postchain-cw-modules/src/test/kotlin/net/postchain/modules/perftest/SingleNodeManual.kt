// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.modules.perftest

import net.postchain.common.TimeLog
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.PostchainTestNode
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
class SingleNodeManual : IntegrationTestSetup() {

    @Test
    fun runSingleFTNode() {
        runSingleNode("FT", "/net/postchain/single_ft_node/blockchain_config.xml")
    }

    @Test
    fun runSingleGtxTestNode() {
        runSingleNode("GtxTest", "/net/postchain/single_gtx_node/blockchain_config.xml")
    }

    private fun runSingleNode(name: String, blockchainConfig: String) {
        configOverrides.setProperty("api.port", 8383)
        val nodes = createNodes(1, blockchainConfig)
        val node = nodes[0]

        // warm up
        val warmupDuration = 30000
        var warmupEndTime = System.currentTimeMillis() + warmupDuration
        while (warmupEndTime > System.currentTimeMillis()) {
            Thread.sleep(100)
            //buildBlockAndCommit(node)
        }
        val warmup = txCount(node)
        var warmupHeight = warmup.first
        var warmupTxCount = warmup.second

        // Now actual test
        //TimeLog.enable(true)
        val testDuration = 60000
        var endTime = System.currentTimeMillis() + testDuration
        while (endTime > System.currentTimeMillis()) {
            Thread.sleep(100)
            //buildBlockAndCommit(node)
        }

        val pair = txCount(node)
        val blocksCount = pair.first - warmupHeight
        var txCount = pair.second - warmupTxCount

        println("==================================================")
        println("Ran $name for $testDuration ms:")

        println(TimeLog.toString())

        println("Total blocks: $blocksCount")
        println("Total transaction count: $txCount")
        println("Avg tx/block: ${txCount / (blocksCount)}")
        println("node tps: ${txCount * 1000 / testDuration}")
        println("buildBlock tps: ${txCount * 1000 / TimeLog.getValue("BaseBlockchainEngine.buildBlock().buildBlock")}")
    }

    private fun txCount(node: PostchainTestNode): Pair<Long, Int> {
        return node.getBlockchainInstance().getEngine().getBlockQueries().let { blockQueries ->
            val bestHeight = blockQueries.getBestHeight().get()
            var txCount = (0..bestHeight).fold(0) { count, height ->
                val blockRid = blockQueries.getBlockRid(height).get()!!
                count + blockQueries.getBlockTransactionRids(blockRid).get().size
            }

            Pair(bestHeight, txCount)
        }
    }
}