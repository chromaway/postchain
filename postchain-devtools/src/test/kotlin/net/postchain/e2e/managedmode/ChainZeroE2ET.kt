package net.postchain.e2e.managedmode

import assertk.assert
import assertk.assertions.contains
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.output.ToStringConsumer
import java.io.File

class ChainZeroE2ET {

    private val dockerComposeFile = File(
            javaClass.getResource("/e2e/test1_node1_launched/docker-compose.yml").file
    )

    private val logConsumer = ToStringConsumer()

    @get:Rule
    val environment: DockerComposeContainer<Nothing> = DockerComposeContainer<Nothing>(dockerComposeFile)
            .withLogConsumer("postchain-mme", logConsumer)

    @Test
    fun test1_node1_launched_successfully() {
        Awaitility.await()
                .atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    assert(logConsumer.toUtf8String())
                            .contains("Postchain node is running")
                }
    }
}