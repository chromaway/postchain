package net.postchain.integrationtest.multiple_chains

import assertk.assertions.isNotNull
import assertk.assertions.isNull
import mu.KLogging
import net.postchain.common.hexStringToByteArray
import net.postchain.devtools.IntegrationTest
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test

class SinglePeerMultipleChainsOperationsTest : IntegrationTest() {

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
        val blockchainRid1 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3".hexStringToByteArray()
        val blockchainConfig1 = readBlockchainConfig(
                "/net/postchain/multiple_chains/chains_ops/single_peer/blockchain_config_1.xml")

        // Asserting that chain 1 is not started
        assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNull()

        // Adding chain 1
        nodes[0].addBlockchain(chainId1, blockchainRid1, blockchainConfig1)
        nodes[0].startBlockchain(chainId1)

        // Asserting chain 1 is started
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNotNull()
                }

        // Stopping chain 1
        nodes[0].stopBlockchain(chainId1)

        // Asserting chain 1 is stopped
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNull()
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
        val blockchainRid1 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3".hexStringToByteArray()
        val blockchainConfig1 = readBlockchainConfig(
                "/net/postchain/multiple_chains/chains_ops/single_peer/blockchain_config_1.xml")

        // chain 2
        val chainId2 = 2L
        val blockchainRid2 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a4".hexStringToByteArray()
        val blockchainConfig2 = readBlockchainConfig(
                "/net/postchain/multiple_chains/chains_ops/single_peer/blockchain_config_2.xml")

        // Asserting that chain1 and chain2 Are not started
        assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNull()
        assertk.assert(nodes[0].retrieveBlockchain(chainId2)).isNull()

        // Adding chain 1
        nodes[0].addBlockchain(chainId1, blockchainRid1, blockchainConfig1)
        nodes[0].startBlockchain(chainId1)
        // Asserting chain 1 is started and chain 2 is not
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNotNull()
                    assertk.assert(nodes[0].retrieveBlockchain(chainId2)).isNull()
                }

        // Adding chain 2
        nodes[0].addBlockchain(chainId2, blockchainRid2, blockchainConfig2)
        nodes[0].startBlockchain(chainId2)
        // Asserting chain 2 is started too
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNotNull()
                    assertk.assert(nodes[0].retrieveBlockchain(chainId2)).isNotNull()
                }

        // Stopping chain 1
        nodes[0].stopBlockchain(chainId1)
        // Asserting chain 1 is stopped and chain 2 is not
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNull()
                    assertk.assert(nodes[0].retrieveBlockchain(chainId2)).isNotNull()
                }

        // Stopping chain 2
        nodes[0].stopBlockchain(chainId2)
        // Asserting chain 1 is stopped and chain 2 is not
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNull()
                    assertk.assert(nodes[0].retrieveBlockchain(chainId2)).isNull()
                }
    }

}