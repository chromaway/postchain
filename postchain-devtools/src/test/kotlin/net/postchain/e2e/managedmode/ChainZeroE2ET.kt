package net.postchain.e2e.managedmode

import assertk.assert
import assertk.assertions.*
import io.restassured.path.json.JsonPath
import mu.KLogging
import net.postchain.common.toHex
import net.postchain.e2e.tools.*
import org.awaitility.Awaitility.await
import org.awaitility.Duration.*
import org.junit.Test


class ChainZeroE2ET : End2EndTests() {

    companion object : KLogging()

    @Test
    fun testDebugInterface() {
        val postgresUrl = postgresUrl(SERVICE_POSTGRES, POSTGRES_PORT)

        // Starting node1 and asserting it is running
        val node1 = buildNode1Container(postgresUrl)
                .apply { start() }
                .also { assertingNodeIsRunning(it) }

        fun assertDebugInfo(debug: JsonPath) {
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Validator")
        }

        // Retrieving debug info via REST API endpoint /_debug
        val restApiTool = RestApiTool(node1.containerIpAddress, node1.getMappedPort(apiPort1))
        await().atMost(TEN_SECONDS).pollInterval(ONE_SECOND).untilAsserted {
            assertDebugInfo(restApiTool.getDebug())
        }

        // The same but via exec-in-container and curl
        val curlTool = CurlTool(node1, apiPort1)
        assertDebugInfo(curlTool.getDebug())

        node1.stop()
    }

    @Test
    fun end2endTests() {
        val postgresUrl = postgresUrl(SERVICE_POSTGRES, POSTGRES_PORT)

        /**
         * Test 1: launch node1
         */
        // Starting node1 and asserting it is running
        val node1 = buildNode1Container(postgresUrl)
                .apply { start() }
                .also { assertingNodeIsRunning(it) }

        /**
         * Test 2: post 5 txs to node1/chain-zero
         */
        val txSender1 = buildChainZeroTxSender(node1, apiPort1, privKey1, pubKey1)
        repeat(5) {
            txSender1.postNopTx()
        }

        val dbTool1 = buildDbTool(postgres, POSTGRES_PORT, postgresDbScheme1)
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getTxsCount()).isEqualTo(5L)
        }

        /**
         * Test 3: launch separate node2 as replica
         */
        // Starting node2 and asserting it is running
        var node2 = buildNode2Container(postgresUrl)
                .apply { start() }
                .also { assertingNodeIsRunning(it) }

        // Waiting until node1 builds a couple of blocks
        var height1: Int? = null
        await().atMost(SECONDS_20).pollInterval(ONE_SECOND).untilAsserted {
            height1 = parseLogLastHeight(node1.logs)
            assert(height1).isNotNull()
        }

        await().atMost(SECONDS_20).pollInterval(ONE_SECOND).untilAsserted {
            val height12 = parseLogLastHeight(node1.logs)!!
            assert(height12).isNotNull()
            assert(height12).isGreaterThan(height1!!)
        }

        // Asserting node2 doesn't build blocks b/c it's a replica and doesn't receive any blocks
        await().atMost(SECONDS_20).pollInterval(ONE_SECOND).untilAsserted {
            assert(parseLogLastHeight(node2.logs)).isNull()
        }

        // * waiting for 20 sec
        await().pollDelay(SECONDS_20).atMost(SECONDS_21).pollInterval(ONE_SECOND).untilAsserted {
            assert(parseLogLastHeight(node2.logs)).isNull()
        }

        /**
         * Test 4: post 5 nop txs to node1/chain-zero
         */
        repeat(5) {
            txSender1.postNopTx()
        }

        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getTxsCount()).isEqualTo(5L + 5L)
        }

        val dbTool2 = buildDbTool(postgres, POSTGRES_PORT, postgresDbScheme2)
        await().pollDelay(SECONDS_20).atMost(SECONDS_21).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool2.getTxsCount()).isEqualTo(0L) // No txs
        }

        /**
         * Test 5: include node2 as replica to the network of node1
         */
        // Asserting node1 doesn't know any peers
        assert(dbTool1.getPeerIds()).isEmpty()

        // Posting add-peer-replica(node2) tx
        txSender1.postAddPeerAsReplicaTx(pubKey1, SERVICE_NODE2, port2, pubKey2)

        // Asserting node1 knows only node2
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            val peers = dbTool1.getPeerIds()
            assert(peers).hasSize(1)
            assert(peers.first().toHex().toUpperCase())
                    .isEqualTo(pubKey2.toUpperCase())
        }

        // Asserting node2 received all txs from node1: 11 = 10 (nop) + 1 (add-peer)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool2.getTxsCount()).isEqualTo(10L + 1L)
        }

        /**
         * Test 6: stop and run again node2
         */
        // Stopping node2
        node2.dockerClient.stopContainerCmd(node2.containerId).exec()

        // Posting 5 nop txs to node1
        repeat(5) {
            txSender1.postNopTx()
        }

        // Asserting that node1 has 16 = 10 + 1 + 5 txs
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getTxsCount()).isEqualTo(16L)
        }

        // Starting node2 again
        node2.dockerClient.startContainerCmd(node2.containerId).exec()

        // Asserting node2 received all txs from node1
        await().pollDelay(TEN_SECONDS).atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool2.getTxsCount()).isEqualTo(16L)
        }

        /**
         * Test 7: stop and run again node2 with wiping db
         */
        // Stopping node2 and removing its container
        node2.stop()

        // Posting 5 nop txs to node1
        repeat(5) {
            txSender1.postNopTx()
        }

        // Asserting that node1 has 21 = 16 + 5 txs
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getTxsCount()).isEqualTo(21L)
        }

        // Starting node2 (new) with wiping DB
        node2 = buildNode2Container(postgresUrl, true)
                .apply { start() }

        // Asserting node2 (new) is running
        assertingNodeIsRunning(node2)

        // Asserting node2 (new) received all txs from node1
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool2.getTxsCount()).isEqualTo(21L)
        }


        /**
         * Test 8: launch separate node3 as replica
         */
        // Starting node3 and asserting it is running
        val node3 = buildNode3Container(postgresUrl)
                .apply { start() }
                .also { assertingNodeIsRunning(it) }

        // Waiting until node1 builds a couple of blocks and node2 (new) receives them
        var height21: Int? = null
        await().atMost(SECONDS_20).pollInterval(ONE_SECOND).untilAsserted {
            height21 = parseLogLastHeight(node2.logs)
            assert(height21).isNotNull()
        }

        await().atMost(SECONDS_20).pollInterval(ONE_SECOND).untilAsserted {
            val height22 = parseLogLastHeight(node2.logs)!!
            assert(height22).isNotNull()
            assert(height22).isGreaterThan(height21!!)
        }

        // Asserting node3 doesn't build blocks b/c it's a replica and doesn't receive any blocks
        await().atMost(SECONDS_20).pollInterval(ONE_SECOND).untilAsserted {
            assert(parseLogLastHeight(node3.logs)).isNull()
        }

        // * waiting for 20 sec
        await().pollDelay(SECONDS_20).atMost(SECONDS_21).pollInterval(ONE_SECOND).untilAsserted {
            assert(parseLogLastHeight(node3.logs)).isNull()
        }


        /**
         * Test 9: include node3 as replica to the network of node1 and node2
         */
        // At this point node1 knows only node2 (see. Test 5)

        // Posting add-peer-replica(node3) tx
        txSender1.postAddPeerAsReplicaTx(pubKey1, SERVICE_NODE3, port3, pubKey3)

        // Asserting node1 knows node2 and node3
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            val peers = dbTool1.getPeerIds().map { it.toHex().toUpperCase() }
            assert(peers).hasSize(2)
            assert(peers).contains(pubKey2.toUpperCase())
            assert(peers).contains(pubKey3.toUpperCase())
        }

        // Asserting node3 received all txs from the network: 22 = 21 + 1 (add-peer)
        val dbTool3 = buildDbTool(postgres, POSTGRES_PORT, postgresDbScheme3)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool3.getTxsCount()).isEqualTo(22L)
        }


        /**
         * Test 10: make node2 as signer (reconfigure the network)
         */
        // Asserting chain-zero doesn't know any blockchain's configs
        assert(dbTool1.getBlockchainConfigsHeights()).isEmpty()

        // Adding chain-zero's config at height 0.
        // It's an initial operation to insert record to c0.blockchain table.
        txSender1.postAddBlockchainConfigurationTx(
                readResourceFile("/e2e/chain-zero/test10/00.gtv"), 0)

        // Asserting chain-zero knows about single config
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getBlockchainConfigsHeights()).hasSize(1)
        }

        // Asserting node1 is signer and node2 and node3 are replicas via /_debug REST API
        val restApiTool1 = RestApiTool(node1.containerIpAddress, node1.getMappedPort(apiPort1))
        await().atMost(TEN_SECONDS).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool1.getDebug()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Validator")
        }

        var restApiTool2 = RestApiTool(node2.containerIpAddress, node2.getMappedPort(apiPort2))
        await().atMost(TEN_SECONDS).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool2.getDebug()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Replica")
        }

        val restApiTool3 = RestApiTool(node3.containerIpAddress, node3.getMappedPort(apiPort3))
        await().atMost(TEN_SECONDS).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool3.getDebug()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Replica")
        }

        // Retrieving current height for node1/chain-zero:
        val currentHeight0 = parseLogLastHeight(node1.logs)!!
        // Adding chain-zero's config at height (currentHeight0 + 3).
        txSender1.postAddBlockchainConfigurationTx(
                readResourceFile("/e2e/chain-zero/test10/01.gtv"), currentHeight0 + 3)

        // Asserting chain-zero knows about two configs
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getBlockchainConfigsHeights()).hasSize(2)
        }

        // Asserting node1 and node2 are signers and node3 is replica via /_debug REST API
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool1.getDebug()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Validator")
        }

        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool2.getDebug()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Validator")
        }

        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool3.getDebug()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Replica")
        }


        /**
         * Test 11: stop node2 for some time
         */
        // Asserting the network builds blocks
        var height11_1: Int? = parseLogLastHeight(node1.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node1.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height11_1!!)
        }

        var height11_2: Int? = parseLogLastHeight(node2.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node2.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height11_2!!)
        }

        var height11_3: Int? = parseLogLastHeight(node3.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node3.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height11_3!!)
        }

        // Stopping node2
        node2.dockerClient.stopContainerCmd(node2.containerId).exec()

        // Waiting for 10 seconds to until network stops
        await().pollDelay(TEN_SECONDS).atMost(SECONDS_11).until { true }

        // Asserting the network [node1, node3] is stopped
        // - node1
        height11_1 = parseLogLastHeight(node1.logs)!!
        await().pollDelay(SECONDS_20).atMost(SECONDS_21).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node1.logs)!!
            assert(height).isNotNull()
            assert(height).isEqualTo(height11_1)
        }

        // - node3
        height11_3 = parseLogLastHeight(node3.logs)!!
        await().pollDelay(SECONDS_20).atMost(SECONDS_21).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node3.logs)!!
            assert(height).isNotNull()
            assert(height).isEqualTo(height11_3)
        }

        // Starting node2 again
        node2.dockerClient.startContainerCmd(node2.containerId).exec()

        // Asserting the network builds blocks again
        height11_1 = parseLogLastHeight(node1.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node1.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height11_1!!)
        }

        height11_2 = parseLogLastHeight(node2.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node2.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height11_2!!)
        }

        height11_3 = parseLogLastHeight(node3.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node3.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height11_3!!)
        }


        /**
         * Test 12: make node3 as signer (reconfigure the network) (cf. Test 10)
         */
        // Retrieving current height for node1/chain-zero:
        val height12 = parseLogLastHeight(node1.logs)!!
        // Adding chain-zero's config at height (height12 + 3).
        txSender1.postAddBlockchainConfigurationTx(
                readResourceFile("/e2e/chain-zero/test10/02.gtv"), height12 + 3)

        // Asserting chain-zero knows about tree configs (initial 00.gtv, 01.gtv, 02.gtv)
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getBlockchainConfigsHeights()).hasSize(3)
        }

        // Asserting node1, node2 and node3 are signers via /_debug REST API
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool1.getDebug()
            assert(debug.getString("version")).isNotNull()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Validator")
        }

        // To get debug info we use curl via exec-in-container b/c
        // after stop/start of container exposed ports are broken.
        val curlTool2 = CurlTool(node2, apiPort2)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val debug = curlTool2.getDebug()
            assert(debug.getString("version")).isNotNull()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Validator")
        }

        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool3.getDebug()
            assert(debug.getString("version")).isNotNull()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Validator")
        }


        /**
         * Test 13: stop node3 for some time (cf. Test 11)
         */
        // Asserting the network builds blocks
        var height13_1: Int? = parseLogLastHeight(node1.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node1.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height13_1!!)
        }

        var height13_2: Int? = parseLogLastHeight(node2.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node2.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height13_2!!)
        }

        var height13_3: Int? = parseLogLastHeight(node3.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node3.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height13_3!!)
        }

        // Stopping node3
        node3.dockerClient.stopContainerCmd(node3.containerId).exec()

        // Waiting for 10 seconds to until network stops
        await().pollDelay(TEN_SECONDS).atMost(SECONDS_11).until { true }

        // Asserting the network [node1, node2] is stopped
        // - node1
        height13_1 = parseLogLastHeight(node1.logs)!!
        await().pollDelay(SECONDS_20).atMost(SECONDS_21).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node1.logs)!!
            assert(height).isNotNull()
            assert(height).isEqualTo(height13_1)
        }

        // - node2
        height13_2 = parseLogLastHeight(node2.logs)!!
        await().pollDelay(SECONDS_20).atMost(SECONDS_21).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node2.logs)!!
            assert(height).isNotNull()
            assert(height).isEqualTo(height13_2)
        }

        // Starting node3 again
        node3.dockerClient.startContainerCmd(node3.containerId).exec()

        // Asserting the network builds blocks again
        height13_1 = parseLogLastHeight(node1.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node1.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height13_1!!)
        }

        height13_2 = parseLogLastHeight(node2.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node2.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height13_2!!)
        }

        height13_3 = parseLogLastHeight(node3.logs)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val height = parseLogLastHeight(node3.logs)!!
            assert(height).isNotNull()
            assert(height).isGreaterThan(height13_3!!)
        }


        // End of tests
        // - stopping nodes
        node1.stop()
        node2.stop()
        node3.stop()
        // - closing DbTool-s
        dbTool1.close()
        dbTool2.close()
        dbTool3.close()
    }

}