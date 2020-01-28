// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class FullEbftWithReplicasTestNightly : FullEbftTestNightlyCore() {

    @Ignore // TODO: [et]: Fix this tests
    @Test
    @Parameters(
            "3, 1, 0, 1",
            "3, 10, 4, 3"
    )
    @TestCaseName("[{index}] nodesCount: {0}, blocksCount: {1}, txPerBlock: {2}, replicasCount: {3}")
    fun runXNodesWithYTxPerBlockAndReplica(nodesCount: Int, blocksCount: Int, txPerBlock: Int, replicasCount: Int) {
        logger.info {
            "runXNodesWithYTxPerBlockAndReplica(): " +
                    "nodesCount: $nodesCount, " +
                    "blocksCount: $blocksCount, " +
                    "txPerBlock: $txPerBlock, " +
                    "replicasCount: $replicasCount"
        }

        configOverrides.setProperty("testpeerinfos", createPeerInfosWithReplicas(nodesCount, replicasCount))
        createNodesWithReplicas(nodesCount, replicasCount, "/net/postchain/full_ebft/blockchain_config_$nodesCount.xml")

        runXNodesWithYTxPerBlockTest(blocksCount, txPerBlock)
    }
}
