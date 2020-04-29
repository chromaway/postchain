package net.postchain.e2e.managedmode

import assertk.assertions.contains
import net.postchain.e2e.tools.*
import org.awaitility.Awaitility.await
import org.awaitility.Duration.ONE_MINUTE
import org.awaitility.Duration.ONE_SECOND
import org.junit.Rule
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

abstract class End2EndTests {

    // Docker
    protected val POSTGRES_DOCKER_IMAGE = "chromaway/postgres:2.4.3-beta"
    protected val POSTCHAIN_DOCKER_IMAGE = "chromaway/postchain-mme:3.2.0"

    // Env
    protected val ENV_POSTCHAIN_DB_URL = "POSTCHAIN_DB_URL"
    protected val ENV_NODE = "ENV_NODE"
    protected val ENV_WIPE_DB = "WIPE_DB"
    protected val POSTGRES_PORT = 5432

    // Node1
    protected val port1 = 9871
    protected val apiPort1 = 7741
    protected val privKey1 = "3132333435363738393031323334353637383930313233343536373839303131"
    protected val pubKey1 = "0350fe40766bc0ce8d08b3f5b810e49a8352fdd458606bd5fafe5acdcdc8ff3f57"
    protected val postgresDbScheme1 = "mme_node1"

    // Node2
    protected val port2 = 9872
    protected val apiPort2 = 7742
    protected val privKey2 = "30807728c8c207c48f6d03c414177ca2c04e92fa683d2d1dc0dcaea6ae3c6240"
    protected val pubKey2 = "02b99a05912b01b7797d84d6660e9ed35faee078bd5bdf40026e0cc6e0cb2ef50c"
    protected val postgresDbScheme2 = "mme_node2"

    // Node3
    protected val port3 = 9873
    protected val apiPort3 = 7743
    protected val privKey3 = "3AFAED9C68D6DB2013DD56554EE69A3C9B1E2AAC112F534B12A5FD4B7928B376"
    protected val pubKey3 = "02839DDE1D2121CE72794E54180F5F5C3AD23543D419CB4C3640A854ACB1ADA9E6"
    protected val postgresDbScheme3 = "mme_node3"

    // Blockchains
    protected val blockchainRid0 = "D9A1466EEE33A01293FE9FE8BE2E5BAF502AF37B7A3E138D9D267D710E146626"
    protected val blockchainRidCity = "0C0A20BA590074B034F169ADB4F89D0DCC376C0BAB09CDAB4A9747CFC031EF2D"

    // Networking
    protected val network = Network.newNetwork()
    protected val SERVICE_POSTGRES = "network3_postgres"
    protected val SERVICE_NODE1 = "network3_node1"
    protected val SERVICE_NODE2 = "network3_node2"
    protected val SERVICE_NODE3 = "network3_node3"

    @get:Rule
    val postgres: KGenericContainer = KGenericContainer(POSTGRES_DOCKER_IMAGE)
            .withNetwork(network)
            .withNetworkAliases(SERVICE_POSTGRES)
            .withExposedPorts(POSTGRES_PORT)
            .waitingFor(
                    Wait.forLogMessage(".*database system is ready to accept connections.*", 1)
            )

    protected fun buildNode1Container(postgresUrl: String): KGenericContainer {
        return KGenericContainer(POSTCHAIN_DOCKER_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(SERVICE_NODE1)
                .withExposedPorts(apiPort1)
                .withEnv(ENV_POSTCHAIN_DB_URL, postgresUrl)
                .withEnv(ENV_NODE, "node1")
    }

    protected fun buildNode2Container(postgresUrl: String, wipeDb: Boolean = false): KGenericContainer {
        return KGenericContainer(POSTCHAIN_DOCKER_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(SERVICE_NODE2)
                .withExposedPorts(apiPort2)
                .withEnv(ENV_POSTCHAIN_DB_URL, postgresUrl)
                .withEnv(ENV_WIPE_DB, wipeDb.toString())
                .withEnv(ENV_NODE, "node2") // It's necessary to use node2 config in postchain-mme docker
    }

    protected fun buildNode3Container(postgresUrl: String): KGenericContainer {
        return KGenericContainer(POSTCHAIN_DOCKER_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(SERVICE_NODE3)
                .withExposedPorts(apiPort3)
                .withEnv(ENV_POSTCHAIN_DB_URL, postgresUrl)
                .withEnv(ENV_NODE, "node3") // It's necessary to use node3 config in postchain-mme docker
    }

    protected fun buildChainZeroTxSender(node: KGenericContainer, port: Int, privKey: String, pubKey: String): ChainZeroTxSender {
        val exposedHost = node.containerIpAddress
        val exposedPort = node.getMappedPort(port)
        val serviceUrl = "http://$exposedHost:$exposedPort"

        return ChainZeroTxSender(serviceUrl, blockchainRid0, privKey, pubKey)
    }

    protected fun buildCityTxSender(node: KGenericContainer, port: Int, privKey: String, pubKey: String): CityTxSender {
        val exposedHost = node.containerIpAddress
        val exposedPort = node.getMappedPort(port)
        val serviceUrl = "http://$exposedHost:$exposedPort"

        return CityTxSender(serviceUrl, blockchainRidCity, privKey, pubKey)
    }

    protected fun buildDbTool(node: KGenericContainer, port: Int, dbScheme: String): DbTool {
        val exposedHost = node.containerIpAddress
        val exposedPort = node.getMappedPort(port)

        return DbTool(
                postgresUrl(exposedHost, exposedPort),
                dbScheme)
    }

    protected fun readResourceFile(path: String): ByteArray {
        return File(
                javaClass.getResource(path).file
        ).readBytes()
    }

    protected fun assertingNodeIsRunning(node: KGenericContainer) {
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assertk.assert(node.logs).contains("Postchain node is running")
        }
    }

}