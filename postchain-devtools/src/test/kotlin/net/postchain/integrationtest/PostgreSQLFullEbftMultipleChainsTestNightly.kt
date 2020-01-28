// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import net.postchain.integrationtest.multiple_chains.FullEbftMultipleChainsTestNightly
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class PostgreSQLFullEbftMultipleChainsTestNightly : FullEbftMultipleChainsTestNightly() {

    @Test
    @Parameters(
            "1, 0", "2, 0", "10, 0"
            , "1, 1", "2, 1", "10, 1"
            , "1, 10", "2, 10", "10, 10"
    )
    @TestCaseName("[{index}] nodesCount: 1, blocksCount: {0}, txPerBlock: {1}")
    fun runSingleNodeWithYTxPerBlock(blocksCount: Int, txPerBlock: Int) {
        runXNodesWithYTxPerBlock(
                1,
                blocksCount,
                txPerBlock,
                arrayOf(
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/single_node/node0.properties"
                ),
                arrayOf(
                        "/net/postchain/devtools/multiple_chains/ebft_nightly/single_node/blockchain_config_1.xml",
                        "/net/postchain/devtools/multiple_chains/ebft_nightly/single_node/blockchain_config_2.xml"
                ))
    }

    @Test
    @Ignore // TODO: Ignored due to the fact tests often fail
    @Parameters(
            "1, 0", "2, 0", "10, 0"
            , "1, 1", "2, 1", "10, 1"
            , "1, 10", "2, 10", "10, 10"
    )
    @TestCaseName("[{index}] nodesCount: 2, blocksCount: {0}, txPerBlock: {1}")
    fun runTwoNodesWithYTxPerBlock(blocksCount: Int, txPerBlock: Int) {
        runXNodesWithYTxPerBlock(
                2,
                blocksCount,
                txPerBlock,
                arrayOf(
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/two_nodes/node0.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/two_nodes/node1.properties"
                ),
                arrayOf(
                        "/net/postchain/devtools/multiple_chains/ebft_nightly/two_nodes/blockchain_config_1.xml",
                        "/net/postchain/devtools/multiple_chains/ebft_nightly/two_nodes/blockchain_config_2.xml"
                ))
    }

    @Test
    @Ignore
    @Parameters(
            "1, 0", "2, 0", "10, 0"
            , "1, 1", "2, 1", "10, 1"
            , "1, 10", "2, 10", "10, 10"
    )
    @TestCaseName("[{index}] nodesCount: 5, blocksCount: {0}, txPerBlock: {1}")
    fun runFiveNodesWithYTxPerBlock(blocksCount: Int, txPerBlock: Int) {
        runXNodesWithYTxPerBlock(
                5,
                blocksCount,
                txPerBlock,
                arrayOf(
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/node0.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/node1.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/node2.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/node3.properties",
                        "classpath:/net/postchain/multiple_chains/ebft_nightly/five_nodes/node4.properties"
                ),
                arrayOf(
                        "/net/postchain/devtools/multiple_chains/ebft_nightly/five_nodes/blockchain_config_1.xml",
                        "/net/postchain/devtools/multiple_chains/ebft_nightly/five_nodes/blockchain_config_2.xml"
                ))
    }

}