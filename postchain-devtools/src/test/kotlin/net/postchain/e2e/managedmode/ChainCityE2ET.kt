package net.postchain.e2e.managedmode

import assertk.assert
import assertk.assertions.containsAll
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.restassured.path.json.JsonPath
import net.postchain.e2e.tools.RestApiTool
import net.postchain.e2e.tools.parseLogLastHeight
import net.postchain.e2e.tools.postgresUrl
import org.awaitility.Awaitility.await
import org.awaitility.Duration.*
import org.junit.Test

class ChainCityE2ET : End2EndTests() {

    @Test
    fun test() {
        val postgresUrl = postgresUrl(SERVICE_POSTGRES, POSTGRES_PORT)

        // *** GIVEN ***

        // Launching chain-zero on network [node1:validator, node2:replica, node3:replica]
        // - starting node1
        val node1 = buildNode1Container(postgresUrl)
                .apply { start() }
                .also { assertingNodeIsRunning(it) }

        // - starting node2
        val node2 = buildNode2Container(postgresUrl)
                .apply { start() }
                .also { assertingNodeIsRunning(it) }

        // - starting node3
        val node3 = buildNode3Container(postgresUrl)
                .apply { start() }
                .also { assertingNodeIsRunning(it) }

        // Adding node2 and node3 to the network
        val txSender1 = buildChainZeroTxSender(node1, apiPort1, privKey1, pubKey1)
        txSender1.postAddPeerAsReplicaTx(pubKey1, SERVICE_NODE2, port2, pubKey2)
        txSender1.postAddPeerAsReplicaTx(pubKey1, SERVICE_NODE3, port3, pubKey3)

        // Posting 5 txs to node1
        repeat(5) {
            txSender1.postNopTx()
        }

        // Asserting that node2 and node3 receives 5 txs
        val dbTool2 = buildDbTool(postgres, POSTGRES_PORT, postgresDbScheme2)
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool2.getTxsCount()).isEqualTo(2 + 5L)
        }

        val dbTool3 = buildDbTool(postgres, POSTGRES_PORT, postgresDbScheme3)
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool3.getTxsCount()).isEqualTo(2 + 5L)
        }

        // Reconfigure network to [node1:validator, node2:validator, node3:validator]
        // - Adding chain-zero's config at height 0.
        // - It's an initial operation to insert record to c0.blockchain table.
        txSender1.postAddBlockchainConfigurationTx(
                readResourceFile("/e2e/chain-city/00.gtv"), 0)

        // Retrieving current height for node1/chain-zero:
        val currentHeight0 = parseLogLastHeight(node1.logs)!!
        // Adding chain-zero's config at height (currentHeight0 + 3).
        txSender1.postAddBlockchainConfigurationTx(
                readResourceFile("/e2e/chain-city/02.gtv"), currentHeight0 + 3)

        // Asserting node1 and node2 are signers and node3 is replica via /_debug REST API
        val restApiTool1 = RestApiTool(node1.containerIpAddress, node1.getMappedPort(apiPort1))
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool1.getDebug()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Validator")
        }

        val restApiTool2 = RestApiTool(node2.containerIpAddress, node2.getMappedPort(apiPort2))
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool2.getDebug()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Validator")
        }

        val restApiTool3 = RestApiTool(node3.containerIpAddress, node3.getMappedPort(apiPort3))
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            val debug = restApiTool3.getDebug()
            assert(debug.getList<Map<String, String>>("blockchain")).hasSize(1)
            assert(debug.getString("blockchain[0].blockchain-rid").toUpperCase()).isEqualTo(blockchainRid0.toUpperCase())
            assert(debug.getString("blockchain[0].node-type")).isEqualTo("Validator")
        }

        // Posting 5 txs to node2
        val txSender2 = buildChainZeroTxSender(node2, apiPort2, privKey2, pubKey2)
        repeat(5) {
            txSender2.postNopTx()
        }

        // Asserting that node1 and node3 receives 5 txs
        val dbTool1 = buildDbTool(postgres, POSTGRES_PORT, postgresDbScheme1)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getTxsCount()).isEqualTo(2 + 2 + 10L)
        }

        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool3.getTxsCount()).isEqualTo(2 + 2 + 10L)
        }

        /**
         * Test 14: deploy city dapp into the network [node1:validator, node2:replica, node3:replica]
         */

        // *** WHEN ***

        /*
        Deploying chain-city dapp into the network [node1, node2, node3].
        We can't deploy chain-city dapp to node1 only b/c according to the GIVEN section
        node2 and node3 are replicas for the whole node1 and not for chain-zero only
        (see .postAddPeerAsReplicaTx() calls)
         */
        txSender1.postAddBlockchainConfigurationTx(
                blockchainRidCity, readResourceFile("/e2e/chain-city/10.gtv"), 0)

        // *** THEN ***

        // Asserting that node1 runs chain-zero as Validator and runs chain-city as Validator too
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assertNodeRunsChainZeroAndChainCity(
                    restApiTool1.getDebug(), "Validator", "Validator")
        }

        // Asserting that node2 runs chain-zero as Validator and runs chain-city as Replica
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assertNodeRunsChainZeroAndChainCity(
                    restApiTool2.getDebug(), "Validator", "Replica")
        }

        // Asserting that node3 runs chain-zero as Validator and runs chain-city as Replica
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assertNodeRunsChainZeroAndChainCity(
                    restApiTool3.getDebug(), "Validator", "Replica")
        }


        /**
         * Test 15: add 2 cities to the city dapp via node1
         */

        // *** WHEN ***

        // Adding cities
        val cityTxSender1 = buildCityTxSender(node1, apiPort1, privKey1, pubKey1)
        cityTxSender1.postAddCityTx("Stockholm")
        cityTxSender1.postAddCityTx("New York")

        // *** THEN ***

        // Asserting that all peers have the same list of cities
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            val expected = arrayOf("Stockholm", "New York")

            val actual1 = dbTool1.getCities()
            assert(actual1).hasSize(expected.size)
            assert(actual1).containsAll(*expected)

            val actual2 = dbTool2.getCities()
            assert(actual2).hasSize(expected.size)
            assert(actual2).containsAll(*expected)

            val actual3 = dbTool3.getCities()
            assert(actual3).hasSize(expected.size)
            assert(actual3).containsAll(*expected)
        }


        /**
         *
         */

        // *** WHEN ***

        // Adding a new configuration where node1 and node2 are validators and node3 is replica
        // - retrieving current height for node1/chain-zero:
        val currentHeight1 = parseLogLastHeight(node1.logs, "[0C:2D]")!!
        // - adding chain-city's config at height (currentHeight1 + 5).
        // We use '+5' (not '+3') b/c BlockchainConfig::maxblocktime = 2000 (not 5000)
        txSender1.postAddBlockchainConfigurationTx(
                blockchainRidCity, readResourceFile("/e2e/chain-city/11.gtv"), currentHeight1 + 5)


        // *** THEN ***

        // Asserting that node1 runs chain-zero as Validator and runs chain-city as Validator too
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assertNodeRunsChainZeroAndChainCity(
                    restApiTool1.getDebug(), "Validator", "Validator")
        }

        // Asserting that node2 runs chain-zero as Validator and runs chain-city as Validator too
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assertNodeRunsChainZeroAndChainCity(
                    restApiTool2.getDebug(), "Validator", "Validator")
        }

        // Asserting that node3 runs chain-zero as Validator and runs chain-city as Replica
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assertNodeRunsChainZeroAndChainCity(
                    restApiTool3.getDebug(), "Validator", "Replica")
        }


        /**
         * Test 17: add 2 cities to the city dapp via node2
         */

        // *** WHEN ***

        // Adding cities
        val cityTxSender2 = buildCityTxSender(node2, apiPort2, privKey2, pubKey2)
        cityTxSender2.postAddCityTx("Berlin")
        cityTxSender2.postAddCityTx("Paris")

        // *** THEN ***

        // Asserting that all peers have the same list of cities
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            val expected = arrayOf("Stockholm", "New York", "Berlin", "Paris")

            val actual1 = dbTool1.getCities()
            assert(actual1).hasSize(expected.size)
            assert(actual1).containsAll(*expected)

            val actual2 = dbTool2.getCities()
            assert(actual2).hasSize(expected.size)
            assert(actual2).containsAll(*expected)

            val actual3 = dbTool3.getCities()
            assert(actual3).hasSize(expected.size)
            assert(actual3).containsAll(*expected)
        }

    }

    private fun assertNodeRunsChainZeroAndChainCity(debug: JsonPath, chainZeroNodeType: String, chainCityNodeType: String) {
        assert(debug.getList<Map<String, String>>("blockchain")).hasSize(2)

        /*
        RestAssured uses GPath instead of JsonPath
        See https://github.com/rest-assured/rest-assured/issues/969
        val filter0 = "blockchain[?(@[\"blockchain-rid\"]=='$blockchainRid0')]"
         */
        assert(
                debug.getString("blockchain.find { it.'blockchain-rid' == '$blockchainRid0' }.node-type")
        ).isEqualTo(chainZeroNodeType)

        assert(
                debug.getString("blockchain.find { it.'blockchain-rid' == '$blockchainRidCity' }.node-type")
        ).isEqualTo(chainCityNodeType)
    }

}