package net.postchain.e2e.docker

import assertk.assert
import assertk.assertions.contains
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.GenericContainer


class PostgresContainerE2ET {

    @get:Rule
    var postgres = GenericContainer<Nothing>("chromaway/postgres:2.4.3-beta")

    @Test
    fun launch() {
        Awaitility.await()
                .atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    assert(postgres.logs).contains("database system is ready to accept connections")
                }
    }
}