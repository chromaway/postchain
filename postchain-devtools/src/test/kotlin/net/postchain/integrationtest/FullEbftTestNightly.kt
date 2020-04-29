// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class FullEbftTestNightly : FullEbftTestNightlyCore() {

    @Test
    @Parameters(
            "3, 1, 0", "3, 2, 0", "3, 10, 0", "3, 1, 10", "3, 2, 10", "3, 10, 10"
            , "4, 1, 0", "4, 2, 0", "4, 10, 0", "4, 1, 10", "4, 2, 10", "4, 10, 10"
            , "8, 1, 0", "8, 2, 0", "8, 10, 0", "8, 1, 10", "8, 2, 10", "8, 10, 10"
//            , "25, 100, 0"
    )
    @TestCaseName("[{index}] nodesCount: {0}, blocksCount: {1}, txPerBlock: {2}")
    fun runXNodesWithYTxPerBlock(nodesCount: Int, blocksCount: Int, txPerBlock: Int) {
        logger.info {
            "runXNodesWithYTxPerBlock(): " +
                    "nodesCount: $nodesCount, blocksCount: $blocksCount, txPerBlock: $txPerBlock"
        }

        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        createNodes(nodesCount, "/net/postchain/devtools/full_ebft/blockchain_config_$nodesCount.xml")

        runXNodesWithYTxPerBlockTest(blocksCount, txPerBlock)
    }
}
