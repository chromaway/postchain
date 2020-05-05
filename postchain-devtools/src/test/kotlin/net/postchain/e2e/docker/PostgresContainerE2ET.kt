package net.postchain.e2e.docker

import assertk.assert
import assertk.assertions.contains
import net.postchain.e2e.tools.KGenericContainer
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Rule
import org.junit.Test


class PostgresContainerE2ET {

    @get:Rule
    val postgres = KGenericContainer("chromaway/postgres:2.4.3-beta")

    @Test
    fun launch() {
        Awaitility.await()
                .atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    assert(postgres.logs).contains("database system is ready to accept connections")
                }
    }
}