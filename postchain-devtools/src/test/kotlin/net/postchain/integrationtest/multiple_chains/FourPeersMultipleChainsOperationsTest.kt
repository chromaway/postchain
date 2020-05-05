// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.multiple_chains

import mu.KLogging
import net.postchain.devtools.ConfigFileBasedIntegrationTest
import net.postchain.integrationtest.addBlockchainAndStart
import net.postchain.integrationtest.assertChainNotStarted
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.assertNodeConnectedWith
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Ignore
import org.junit.Test

class FourPeersMultipleChainsOperationsTest : ConfigFileBasedIntegrationTest() {

    companion object : KLogging()

    @Test
    @Ignore
    fun startingAndStoppingAllPeersWithoutAnyChain_Successfully() {
        val nodesCount = 4
        configOverrides.setProperty("testpeerinfos", createPeerInfos(nodesCount))
        val nodeConfigsFilenames = arrayOf(
                "classpath:/net/postchain/multiple_chains/chains_ops/four_peers/node0.properties",
                "classpath:/net/postchain/multiple_chains/chains_ops/four_peers/node1.properties",
                "classpath:/net/postchain/multiple_chains/chains_ops/four_peers/node2.properties",
                "classpath:/net/postchain/multiple_chains/chains_ops/four_peers/node3.properties"
        )

        // chain 1 (peers 0, 1, 2, 3)
        val chainId1 = 1L
        val blockchainConfig1 = readBlockchainConfig(
                "/net/postchain/devtools/multiple_chains/chains_ops/four_peers/blockchain_config_1.xml")

        // chain 2 (peers 0, 1, 2)
        val chainId2 = 2L
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/devtools/multiple_chains/chains_ops/four_peers/blockchain_config_2.xml")

        // Creating all peers w/o chains
        createMultipleChainNodes(nodesCount, nodeConfigsFilenames, arrayOf())

        // Asserting that
        await().pollDelay(Duration.FIVE_SECONDS)
                .atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 1 is not started for all peers
                    nodes[0].assertChainNotStarted(chainId1)
                    nodes[1].assertChainNotStarted(chainId1)
                    nodes[2].assertChainNotStarted(chainId1)
                    nodes[3].assertChainNotStarted(chainId1)

                    // chain 2 is not started for all peers
                    nodes[0].assertChainNotStarted(chainId2)
                    nodes[1].assertChainNotStarted(chainId2)
                    nodes[2].assertChainNotStarted(chainId2)

                    // He we can't assert any network topology
                }

        // Launching chain 1 at all peers
        nodes[0].addBlockchainAndStart(chainId1, blockchainConfig1)
        nodes[1].addBlockchainAndStart(chainId1, blockchainConfig1)
        nodes[2].addBlockchainAndStart(chainId1, blockchainConfig1)
        nodes[3].addBlockchainAndStart(chainId1, blockchainConfig1)
        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 1 is started at all peers
                    nodes[0].assertChainStarted(chainId1)
                    nodes[1].assertChainStarted(chainId1)
                    nodes[2].assertChainStarted(chainId1)
                    nodes[3].assertChainStarted(chainId1)

                    // network topology of chain 1 is that peers 0, 1, 2, 3 are interconnected
                    nodes[0].assertNodeConnectedWith(chainId1, nodes[1], nodes[2], nodes[3])
                    nodes[1].assertNodeConnectedWith(chainId1, nodes[0], nodes[2], nodes[3])
                    nodes[2].assertNodeConnectedWith(chainId1, nodes[1], nodes[0], nodes[3])
                    nodes[3].assertNodeConnectedWith(chainId1, nodes[1], nodes[2], nodes[0])
                }

        // Launching chain 2 at all peers
        nodes[0].addBlockchainAndStart(chainId2, blockchainConfig2)
        nodes[1].addBlockchainAndStart(chainId2, blockchainConfig2)
        nodes[2].addBlockchainAndStart(chainId2, blockchainConfig2)
        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 2 is started at all peers
                    nodes[0].assertChainStarted(chainId2)
                    nodes[1].assertChainStarted(chainId2)
                    nodes[2].assertChainStarted(chainId2)

                    // network topology of chain 2 is that peers 0, 1, 2 are interconnected
                    nodes[0].assertNodeConnectedWith(chainId2, nodes[1], nodes[2])
                    nodes[1].assertNodeConnectedWith(chainId2, nodes[0], nodes[2])
                    nodes[2].assertNodeConnectedWith(chainId2, nodes[1], nodes[0])
                }

        // Stopping chain 1 at peer 0
        nodes[0].stopBlockchain(chainId1)
        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 1 is started at peers 1, 2, 3 and stopped at peer 0
                    nodes[0].assertChainNotStarted(chainId1)
                    nodes[1].assertChainStarted(chainId1)
                    nodes[2].assertChainStarted(chainId1)
                    nodes[3].assertChainStarted(chainId1)

                    // network topology of chain 1 is that peers 0, 1, 2, 3 are interconnected
                    nodes[1].assertNodeConnectedWith(chainId1, nodes[2], nodes[3])
                    nodes[2].assertNodeConnectedWith(chainId1, nodes[1], nodes[3])
                    nodes[3].assertNodeConnectedWith(chainId1, nodes[1], nodes[2])

                    // chain 2 is still started at all peers
                    nodes[0].assertChainStarted(chainId2)
                    nodes[1].assertChainStarted(chainId2)
                    nodes[2].assertChainStarted(chainId2)

                    // network topology of chain 2 is still that peers 0, 1, 2 are interconnected
                    nodes[0].assertNodeConnectedWith(chainId2, nodes[1], nodes[2])
                    nodes[1].assertNodeConnectedWith(chainId2, nodes[0], nodes[2])
                    nodes[2].assertNodeConnectedWith(chainId2, nodes[1], nodes[0])
                }

        // Stopping chain 2 at peer 0
        nodes[0].stopBlockchain(chainId2)
        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 1 is started at all peers
                    nodes[0].assertChainNotStarted(chainId1)
                    nodes[1].assertChainStarted(chainId1)
                    nodes[2].assertChainStarted(chainId1)
                    nodes[3].assertChainStarted(chainId1)

                    // network topology of chain 1 is that peers 0, 1, 2, 3 are interconnected
                    nodes[1].assertNodeConnectedWith(chainId1, nodes[2], nodes[3])
                    nodes[2].assertNodeConnectedWith(chainId1, nodes[1], nodes[3])
                    nodes[3].assertNodeConnectedWith(chainId1, nodes[1], nodes[2])

                    // chain 2 is started at peers 1, 2 and stopped at peer 0
                    nodes[0].assertChainNotStarted(chainId2)
                    nodes[1].assertChainStarted(chainId2)
                    nodes[2].assertChainStarted(chainId2)

                    // network topology of chain 2 is that peers 0, 1, 2 are interconnected
                    nodes[1].assertNodeConnectedWith(chainId2, nodes[2])
                    nodes[2].assertNodeConnectedWith(chainId2, nodes[1])
                }

        // Starting chain 1 at peer 0
        nodes[0].startBlockchain(chainId1)
        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 1 is started at all peers
                    nodes[0].assertChainStarted(chainId1)
                    nodes[1].assertChainStarted(chainId1)
                    nodes[2].assertChainStarted(chainId1)
                    nodes[3].assertChainStarted(chainId1)

                    // network topology of chain 1 is that peers 0, 1, 2, 3 are interconnected
                    nodes[0].assertNodeConnectedWith(chainId1, nodes[1], nodes[2], nodes[3])
                    nodes[1].assertNodeConnectedWith(chainId1, nodes[0], nodes[2], nodes[3])
                    nodes[2].assertNodeConnectedWith(chainId1, nodes[1], nodes[0], nodes[3])
                    nodes[3].assertNodeConnectedWith(chainId1, nodes[1], nodes[2], nodes[0])

                    // chain 2 is still started at peers 1, 2 and stopped at peer 0
                    nodes[0].assertChainNotStarted(chainId2)
                    nodes[1].assertChainStarted(chainId2)
                    nodes[2].assertChainStarted(chainId2)

                    // network topology of chain 2 is still that peers 0, 1, 2 are interconnected
                    nodes[1].assertNodeConnectedWith(chainId2, nodes[2])
                    nodes[2].assertNodeConnectedWith(chainId2, nodes[1])
                }

        // Starting chain 2 at peer 0
        nodes[0].startBlockchain(chainId2)
        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain 1 is started at all peers
                    nodes[0].assertChainStarted(chainId1)
                    nodes[1].assertChainStarted(chainId1)
                    nodes[2].assertChainStarted(chainId1)
                    nodes[3].assertChainStarted(chainId1)

                    // network topology of chain 1 is that peers 0, 1, 2, 3 are interconnected
                    nodes[0].assertNodeConnectedWith(chainId1, nodes[1], nodes[2], nodes[3])
                    nodes[1].assertNodeConnectedWith(chainId1, nodes[0], nodes[2], nodes[3])
                    nodes[2].assertNodeConnectedWith(chainId1, nodes[1], nodes[0], nodes[3])
                    nodes[3].assertNodeConnectedWith(chainId1, nodes[1], nodes[2], nodes[0])

                    // chain 2 is started at all peers
                    nodes[0].assertChainStarted(chainId2)
                    nodes[1].assertChainStarted(chainId2)
                    nodes[2].assertChainStarted(chainId2)

                    // network topology of chain 2 is that peers 0, 1, 2 are interconnected
                    nodes[0].assertNodeConnectedWith(chainId2, nodes[1], nodes[2])
                    nodes[1].assertNodeConnectedWith(chainId2, nodes[0], nodes[2])
                    nodes[2].assertNodeConnectedWith(chainId2, nodes[1], nodes[0])
                }
    }

}