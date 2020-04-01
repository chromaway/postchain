// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.app

import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.Test

class AppConfigTest {

    @Test
    fun testEmptyNodeConfig() {
        val appConfig = AppConfig.fromPropertiesFile(
                javaClass.getResource("/net/postchain/config/empty-node-config.properties").file)

        assertk.assert(appConfig.nodeConfigProvider).isEmpty()
        assertk.assert(appConfig.databaseDriverclass).isEmpty()
        assertk.assert(appConfig.databaseUrl).isEmpty()
        assertk.assert(appConfig.databaseSchema).isEqualTo("public")
        assertk.assert(appConfig.databaseUsername).isEmpty()
        assertk.assert(appConfig.databasePassword).isEmpty()
    }
}