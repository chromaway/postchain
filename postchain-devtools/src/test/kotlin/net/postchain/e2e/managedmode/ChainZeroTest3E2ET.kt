package net.postchain.e2e.managedmode

import assertk.assert
import assertk.assertions.*
import mu.KLogging
import net.postchain.common.toHex
import net.postchain.e2e.tools.*
import org.awaitility.Awaitility.await
import org.awaitility.Duration.*
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.ToStringConsumer
import org.testcontainers.containers.wait.strategy.Wait


class ChainZeroTest3E2ET {

    // Env
    private val ENV_POSTCHAIN_DB_URL = "POSTCHAIN_DB_URL"
    private val ENV_NODE = "ENV_NODE"
    private val POSTGRES_PORT = 5432

    // Node1
    private val logConsumer1 = ToStringConsumer()
    private val port1 = 9871
    private val apiPort1 = 7741
    private val privKey1 = "3132333435363738393031323334353637383930313233343536373839303131"
    private val pubKey1 = "0350fe40766bc0ce8d08b3f5b810e49a8352fdd458606bd5fafe5acdcdc8ff3f57"
    private val postgresDbScheme1 = "mme_node1"

    // Node2
    private val logConsumer2 = ToStringConsumer()
    private val port2 = 9872
    private val apiPort2 = 7742
    private val privKey2 = "30807728c8c207c48f6d03c414177ca2c04e92fa683d2d1dc0dcaea6ae3c6240"
    private val pubKey2 = "02b99a05912b01b7797d84d6660e9ed35faee078bd5bdf40026e0cc6e0cb2ef50c"
    private val postgresDbScheme2 = "mme_node2"

    // Blockchains
    private val blockchainRid0 = "D9A1466EEE33A01293FE9FE8BE2E5BAF502AF37B7A3E138D9D267D710E146626"

    // Networking
    private val network = Network.newNetwork()
    private val SERVICE_POSTGRES = "network3_postgres"
    private val SERVICE_NODE1 = "network3_node1"
    private val SERVICE_NODE2 = "network3_node2"

    @get:Rule
    val postgres: KGenericContainer = KGenericContainer("chromaway/postgres:2.4.3-beta")
            .withNetwork(network)
            .withNetworkAliases(SERVICE_POSTGRES)
            .withExposedPorts(POSTGRES_PORT)
            .waitingFor(
                    Wait.forLogMessage(".*database system is ready to accept connections.*", 1)
            )

    companion object : KLogging()

    @Test
    fun test3_launch_node2_as_replica_on_network() {
        val postgresUrl = postgresUrl(SERVICE_POSTGRES, POSTGRES_PORT)

        // Starting node1
        val node1 = KGenericContainer("chromaway/postchain-mme:3.2.0")
                .withNetwork(network)
                .withNetworkAliases(SERVICE_NODE1)
                .withExposedPorts(apiPort1)
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
                .withNetworkAliases(SERVICE_NODE2)
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
         * Test 3: launch separate node2 as replica
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
        val txSender1 = buildTxSender(node1, apiPort1, privKey1, pubKey1)
        repeat(5) {
            txSender1.postNopTx()
        }

        val dbTool1 = buildDbTool(postgres, POSTGRES_PORT, postgresDbScheme1)
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getTxsCount()).isEqualTo(5L)
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

        // Asserting node2 received all txs from node1: 6 = 5 (nop) + 1 (add-peer)
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool2.getTxsCount()).isEqualTo(5L + 1L)
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

        // Asserting that node1 has 11 = 5 + 1 + 5 txs
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool1.getTxsCount()).isEqualTo(11L)
        }

        // Starting node2 again
        node2.dockerClient.startContainerCmd(node2.containerId).exec()

        // Asserting node2 received all txs from node1: 11 = 6 + 5
        await().atMost(TWO_MINUTES).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool2.getTxsCount()).isEqualTo(11L)
        }


        // Stopping nodes
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