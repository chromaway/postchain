// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.multiple_chains

import mu.KLogging
import net.postchain.devtools.ConfigFileBasedIntegrationTest
import net.postchain.integrationtest.addBlockchainAndStart
import net.postchain.integrationtest.assertChainNotStarted
import net.postchain.integrationtest.assertChainStarted
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test

class SinglePeerMultipleChainsOperationsTest : ConfigFileBasedIntegrationTest() {

    companion object : KLogging()

    @Test
    fun startingAndStoppingSingleChainSuccessfully() {
        // Creating node w/o chains
        createMultipleChainNodes(
                1,
                arrayOf("classpath:/net/postchain/multiple_chains/chains_ops/single_peer/node0.properties"),
                arrayOf())

        // chain 1
        val chainId1 = 1L
        val blockchainConfig1 = readBlockchainConfig(
                "/net/postchain/devtools/multiple_chains/chains_ops/single_peer/blockchain_config_1.xml")

        // Asserting that chain 1 is not started
        nodes[0].assertChainNotStarted(chainId1)

        // Adding chain 1
        nodes[0].addBlockchainAndStart(chainId1, blockchainConfig1)

        // Asserting chain 1 is started
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes[0].assertChainStarted(chainId1)
                }

        // Stopping chain 1
        nodes[0].stopBlockchain(chainId1)

        // Asserting chain 1 is stopped
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes[0].assertChainNotStarted(chainId1)
                }
    }

    @Test
    fun startingAndStoppingTwoChainsSuccessfully() {
        // Creating node w/o chains
        createMultipleChainNodes(
                1,
                arrayOf("classpath:/net/postchain/multiple_chains/chains_ops/single_peer/node0.properties"),
                arrayOf())

        // chain 1
        val chainId1 = 1L
        val blockchainConfig1 = readBlockchainConfig(
                "/net/postchain/devtools/multiple_chains/chains_ops/single_peer/blockchain_config_1.xml")

        // chain 2
        val chainId2 = 2L
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/devtools/multiple_chains/chains_ops/single_peer/blockchain_config_2.xml")

        // Asserting that chain1 and chain2 Are not started
        nodes[0].assertChainNotStarted(chainId1)
        nodes[0].assertChainNotStarted(chainId2)

        // Adding chain 1
        nodes[0].addBlockchainAndStart(chainId1, blockchainConfig1)
        // Asserting chain 1 is started and chain 2 is not
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes[0].assertChainStarted(chainId1)
                    nodes[0].assertChainNotStarted(chainId2)
                }

        // Adding chain 2
        nodes[0].addBlockchainAndStart(chainId2, blockchainConfig2)
        // Asserting chain 2 is started too
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes[0].assertChainStarted(chainId1)
                    nodes[0].assertChainStarted(chainId2)
                }

        // Stopping chain 1
        nodes[0].stopBlockchain(chainId1)
        // Asserting chain 1 is stopped and chain 2 is not
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes[0].assertChainNotStarted(chainId1)
                    nodes[0].assertChainStarted(chainId2)
                }

        // Stopping chain 2
        nodes[0].stopBlockchain(chainId2)
        // Asserting chain 1 is stopped and chain 2 is not
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    nodes[0].assertChainNotStarted(chainId1)
                    nodes[0].assertChainNotStarted(chainId2)
                }
    }

}