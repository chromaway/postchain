package net.postchain.e2e.managedmode

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import net.postchain.e2e.tools.DbTool
import net.postchain.e2e.tools.KDockerComposeContainer
import net.postchain.e2e.tools.TxSender
import org.awaitility.Awaitility.await
import org.awaitility.Duration.ONE_MINUTE
import org.awaitility.Duration.ONE_SECOND
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.output.ToStringConsumer
import java.io.File
import kotlin.random.Random

class ChainZeroE2ET {

    private val POSTCHAIN_SERVICE = "postchain-mme_1"
    private val POSTCHAIN_PORT = 7741

    private val POSTGRES_SERVICE = "postgres_1"
    private val POSTGRES_PORT = 5432
    private val POSTGRES_DB_SCHEME = "mme_node1"

    private val blockchainRid0 = "6357B76B43F8905A2BC35CE40906ACD8DA80DD129C469D93F723B94964DDA9E2"
    private val privKey1 = "3132333435363738393031323334353637383930313233343536373839303131"
    private val pubKey1 = "0350fe40766bc0ce8d08b3f5b810e49a8352fdd458606bd5fafe5acdcdc8ff3f57"

    private val dockerComposeFile = File(
            javaClass.getResource("/e2e/test1_node1_launched/docker-compose.yml").file
    )

    private val logConsumer = ToStringConsumer()

    @get:Rule
    val environment: KDockerComposeContainer = KDockerComposeContainer(dockerComposeFile)
            .withExposedService(POSTCHAIN_SERVICE, POSTCHAIN_PORT)
            .withExposedService(POSTGRES_SERVICE, POSTGRES_PORT)
            .withLogConsumer(POSTCHAIN_SERVICE, logConsumer)

    @Test
    fun test1_launch_node1() {
        await().atMost(ONE_MINUTE).untilAsserted {
            assert(logConsumer.toUtf8String())
                    .contains("Postchain node is running")
        }
    }

    @Test
    fun test2_post_5_txs_to_node1() {
        await().atMost(ONE_MINUTE).untilAsserted {
            assert(logConsumer.toUtf8String())
                    .contains("Postchain node is running")
        }

        val txSender = buildTxSender(POSTCHAIN_SERVICE, POSTCHAIN_PORT)
        repeat(5) {
            txSender.postTx("nop", Random.Default.nextInt(1000).toString())
        }

        val dbTool = buildDbTool(POSTGRES_SERVICE, POSTGRES_PORT, POSTGRES_DB_SCHEME)
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool.getTxsCount()).isEqualTo(5L)
        }
    }

    private fun buildTxSender(service: String, port: Int): TxSender {
        val exposedHost = environment.getServiceHost(service, port)
        val exposedPort = environment.getServicePort(service, port)
        val serviceUrl = "http://$exposedHost:$exposedPort"

        return TxSender(serviceUrl, blockchainRid0, privKey1, pubKey1)
    }

    private fun buildDbTool(service: String, port: Int, dbScheme: String): DbTool {
        val exposedHost = environment.getServiceHost(service, port)
        val exposedPort = environment.getServicePort(service, port)
        val databaseUrl = "jdbc:postgresql://$exposedHost:$exposedPort/postchain"

        return DbTool(databaseUrl, dbScheme)
    }

}