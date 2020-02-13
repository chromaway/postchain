package net.postchain.e2e.managedmode

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import mu.KLogging
import net.postchain.e2e.tools.KGenericContainer
import net.postchain.e2e.tools.TWENTY_SECONDS
import net.postchain.e2e.tools.parseLogLastHeight
import net.postchain.e2e.tools.postgresUrl
import org.awaitility.Awaitility.await
import org.awaitility.Duration.ONE_MINUTE
import org.awaitility.Duration.ONE_SECOND
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.ToStringConsumer
import org.testcontainers.containers.wait.strategy.Wait


class ChainZeroTest3E2ET {

    private val ENV_POSTCHAIN_DB_URL = "POSTCHAIN_DB_URL"
    private val ENV_NODE = "ENV_NODE"
    private val POSTGRES_PORT = 5432
    private val logConsumer1 = ToStringConsumer()
    private val logConsumer2 = ToStringConsumer()

    private val network = Network.newNetwork()
    private val NETWORK_ALIAS = "network3"

    @get:Rule
    val postgres: KGenericContainer = KGenericContainer("chromaway/postgres:2.4.3-beta")
            .withNetwork(network)
            .withNetworkAliases(NETWORK_ALIAS)
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

        // Waiting until node1 builds a couple of blocks
        var height1: Int? = null
        await().atMost(TWENTY_SECONDS).pollInterval(ONE_SECOND).untilAsserted {
            height1 = parseLogLastHeight(logConsumer1.toUtf8String())
            assert(height1).isNotNull()
        }

        await().atMost(TWENTY_SECONDS).pollInterval(ONE_SECOND).untilAsserted {
            val height12 = parseLogLastHeight(logConsumer1.toUtf8String())!!
            assert(height12).isNotNull()
            assert(height12).isGreaterThan(height1!!)
        }

        // Asserting node2 doesn't build blocks b/c it's a replica
        await().atMost(TWENTY_SECONDS).pollInterval(ONE_SECOND).untilAsserted {
            assert(
                    parseLogLastHeight(logConsumer2.toUtf8String())
            ).isNull()
        }

        // * waiting for 20 sec
        await().pollDelay(TWENTY_SECONDS).atMost(TWENTY_SECONDS + ONE_SECOND).pollInterval(ONE_SECOND).untilAsserted {
            assert(
                    parseLogLastHeight(logConsumer2.toUtf8String())
            ).isNull()
        }

        node1.stop()
        node2.stop()
    }
}