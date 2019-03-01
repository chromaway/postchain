package net.postchain.integrationtest

import assertk.assertions.isNotNull
import assertk.assertions.isNull
import mu.KLogging
import net.postchain.common.hexStringToByteArray
import net.postchain.devtools.IntegrationTest
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test

class MultipleChainsOperationsTest : IntegrationTest() {

    companion object : KLogging()

    @Test
    fun chainStartingAndStoppingSuccessfully() {
        // Creating node w/o chains
        createMultipleChainNodes(
                1,
                arrayOf("classpath:/net/postchain/multiple_chains/chains_ops/node0.properties"),
                arrayOf())

        // Chain1
        val chainId1 = 1L
        val blockchainRid1 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3".hexStringToByteArray()
        val blockchainConfig1 = readBlockchainConfig(
                "/net/postchain/multiple_chains/chains_ops/blockchain_config_1.xml")

        // Asserting that Chain1 is not started
        assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNull()

        // Adding Chain1
        nodes[0].addBlockchain(chainId1, blockchainRid1, blockchainConfig1)
        nodes[0].startBlockchain(chainId1)

        // Asserting Chain1 is started
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNotNull()
                }

        // Stopping Chain1
        nodes[0].stopBlockchain(chainId1)

        // Asserting Chain1 is stopped
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    assertk.assert(nodes[0].retrieveBlockchain(chainId1)).isNull()
                }
    }

}