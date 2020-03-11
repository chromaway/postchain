package net.postchain.e2e.managedmode

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import net.postchain.e2e.tools.ChainZeroTxSender
import net.postchain.e2e.tools.DbTool
import net.postchain.e2e.tools.KDockerComposeContainer
import net.postchain.e2e.tools.postgresUrl
import org.awaitility.Awaitility.await
import org.awaitility.Duration.ONE_MINUTE
import org.awaitility.Duration.ONE_SECOND
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.output.ToStringConsumer
import java.io.File

@Deprecated("Test1 and Test2 have been moved to ChainZeroE2ET class")
@Ignore
class ChainZeroTest1And2E2ET {

    private val POSTCHAIN_SERVICE = "postchain-mme_1"
    private val POSTCHAIN_PORT = 7741

    private val POSTGRES_SERVICE = "postgres_1"
    private val POSTGRES_PORT = 5432
    private val POSTGRES_DB_SCHEME = "mme_node1"

    private val blockchainRid0 = "D9A1466EEE33A01293FE9FE8BE2E5BAF502AF37B7A3E138D9D267D710E146626"
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
    @Ignore
    fun test1_launch_node1() {
        await().atMost(ONE_MINUTE).untilAsserted {
            assert(logConsumer.toUtf8String())
                    .contains("Postchain node is running")
        }
    }

    @Test
    @Ignore
    fun test2_post_5_txs_to_node1() {
        await().atMost(ONE_MINUTE).untilAsserted {
            assert(logConsumer.toUtf8String())
                    .contains("Postchain node is running")
        }

        val txSender = buildTxSender(POSTCHAIN_SERVICE, POSTCHAIN_PORT)
        repeat(5) {
            txSender.postNopTx()
        }

        val dbTool = buildDbTool(POSTGRES_SERVICE, POSTGRES_PORT, POSTGRES_DB_SCHEME)
        await().atMost(ONE_MINUTE).pollInterval(ONE_SECOND).untilAsserted {
            assert(dbTool.getTxsCount()).isEqualTo(5L)
        }
    }

    private fun buildTxSender(service: String, port: Int): ChainZeroTxSender {
        val exposedHost = environment.getServiceHost(service, port)
        val exposedPort = environment.getServicePort(service, port)
        val serviceUrl = "http://$exposedHost:$exposedPort"

        return ChainZeroTxSender(serviceUrl, blockchainRid0, privKey1, pubKey1)
    }

    private fun buildDbTool(service: String, port: Int, dbScheme: String): DbTool {
        val exposedHost = environment.getServiceHost(service, port)
        val exposedPort = environment.getServicePort(service, port)

        return DbTool(
                postgresUrl(exposedHost, exposedPort),
                dbScheme)
    }

}