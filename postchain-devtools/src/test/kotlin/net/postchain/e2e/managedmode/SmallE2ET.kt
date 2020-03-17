package net.postchain.e2e.managedmode

import mu.KLogging
import net.postchain.e2e.tools.postgresUrl
import org.junit.Test

class SmallE2ET : End2EndTests() {

    companion object : KLogging()

    @Test
    fun testDebugInterface() {
        val postgresUrl = postgresUrl(SERVICE_POSTGRES, POSTGRES_PORT)

        // Starting node1 and asserting it is running
        val node1 = buildNode1Container(postgresUrl)
                .apply { start() }
                .also { assertingNodeIsRunning(it) }

        node1.stop()
    }

}