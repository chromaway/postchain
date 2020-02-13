package net.postchain.e2e.managedmode

import assertk.assert
import assertk.assertions.*
import mu.KLogging
import net.postchain.e2e.tools.*
import org.awaitility.Awaitility.await
import org.awaitility.Duration.ONE_MINUTE
import org.awaitility.Duration.ONE_SECOND
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.ToStringConsumer
import org.testcontainers.containers.wait.strategy.Wait
import kotlin.random.Random


class ChainZeroTest3E2ET {

    // Env
    private val ENV_POSTCHAIN_DB_URL = "POSTCHAIN_DB_URL"
    private val ENV_NODE = "ENV_NODE"
    private val POSTGRES_PORT = 5432

    // Node1
    private val logConsumer1 = ToStringConsumer()
    private val port1 = 7741
    private val privKey1 = "3132333435363738393031323334353637383930313233343536373839303131"
    private val pubKey1 = "0350fe40766bc0ce8d08b3f5b810e49a8352fdd458606bd5fafe5acdcdc8ff3f57"
    private val postgresDbScheme1 = "mme_node1"

    // Node2
    private val logConsumer2 = ToStringConsumer()
    private val port2 = 7742
    private val privKey2 = "-"
    private val pubKey2 = "-"
    private val postgresDbScheme2 = "mme_node2"

    // Blockchains
    private val blockchainRid0 = "6357B76B43F8905A2BC35CE40906ACD8DA80DD129C469D93F723B94964DDA9E2"

    // Networking
    private val network = Network.newNetwork()
    private val NETWORK_ALIAS = "network3"

    @get:Rule
    val postgres: KGenericContainer = KGenericContainer("chromaway/postgres:2.4.3-beta")
            .withNetwork(network)
            .withNetworkAliases(NETWORK_ALIAS)
            .withExposedPorts(POSTGRES_PORT)
            .waitingFor(
                    Wait.forLogMessage(".*database system is ready to accept connections.*", 1)
            )

    companion object : KLogging()

    @Test
    fun test3_launch_node2_as_replica_on_network() {
        val postgresUrl = postgresUrl(NETWORK_ALIAS, POSTGRES_PORT)

        // Starting node1
        val node1 = KGenericContainer("chromaway/postchain-mme:3.2.0")
                .withNetwork(network)
                .withExposedPorts(port1)
                .withEnv(ENV_POSTCHAIN_DB_URL, postgresUrl)
                .withLogConsumer(logConsumer1)

        node1.start()

        // Asserting node1 is running
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(logConsumer1.toUtf8String())
                    .contains("Postchain node is running")
        }

        // Starting node2
        val node2 = KGenericContainer("chromaway/postchain-mme:3.2.0")
                .withNetwork(network)
                .withEnv(ENV_POSTCHAIN_DB_URL, postgresUrl)
                .withEnv(ENV_NODE, "node2") // It's necessary to use node2 config in postchain-mme docker
                .withLogConsumer(logConsumer2)

        node2.start()

        // Asserting node2 is running
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(logConsumer2.toUtf8String())
                    .contains("Postchain node is running")
        }

        /**
         * Test 3: launch node2 as replica on the network
         */

        // Waiting until node1 builds a couple of blocks
        var height1: Int? = null
        await().atMost(SECONDS_20).pollInterval(ONE_SECOND).untilAsserted {
            height1 = parseLogLastHeight(logConsumer1.toUtf8String())
            assert(height1).isNotNull()
        }

        await().atMost(SECONDS_20).pollInterval(ONE_SECOND).untilAsserted {
            val height12 = parseLogLastHeight(logConsumer1.toUtf8String())!!
            assert(height12).isNotNull()
            assert(height12).isGreaterThan(height1!!)
        }

        // Asserting node2 doesn't build blocks b/c it's a replica
        await().atMost(SECONDS_20).pollInterval(ONE_SECOND).untilAsserted {
            assert(
                    parseLogLastHeight(logConsumer2.toUtf8String())
            ).isNull()
        }

        // * waiting for 20 sec
        await().pollDelay(SECONDS_20).atMost(SECONDS_21).pollInterval(ONE_SECOND).untilAsserted {
            assert(
                    parseLogLastHeight(logConsumer2.toUtf8String())
            ).isNull()
        }

        /**
         * Test 4: post 5 nop txs to node1/chain-zero
         */
        val txSender1 = buildTxSender(node1, port1, privKey1, pubKey1)
        repeat(5) {
            txSender1.postTx("nop", Random.Default.nextInt(1000).toString())
        }

        val dbTool1 = buildDbTool(postgres, POSTGRES_PORT, postgresDbScheme1)
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getTxsCount()).isEqualTo(5L)
        }

        val dbTool2 = buildDbTool(postgres, POSTGRES_PORT, postgresDbScheme2)
        await().pollDelay(SECONDS_20).atMost(SECONDS_21).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool2.getTxsCount()).isEqualTo(0L) // No txs
        }

        node1.stop()
        node2.stop()
    }

    private fun buildTxSender(node: KGenericContainer, port: Int, privKey: String, pubKey: String): TxSender {
        val exposedHost = node.containerIpAddress
        val exposedPort = node.getMappedPort(port)
        val serviceUrl = "http://$exposedHost:$exposedPort"

        return TxSender(serviceUrl, blockchainRid0, privKey, pubKey)
    }

    private fun buildDbTool(node: KGenericContainer, port: Int, dbScheme: String): DbTool {
        val exposedHost = node.containerIpAddress
        val exposedPort = node.getMappedPort(port)

        return DbTool(
                postgresUrl(exposedHost, exposedPort),
                dbScheme)
    }
}