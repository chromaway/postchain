// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest

import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.MapConfiguration

object DummyConfig {

    fun getDummyConfig(): Configuration {
        return MapConfiguration(mapOf(
                "configuration.provider.node" to "legacy",
                "database.driverclass" to "org.postgresql.Driver",
                "database.url" to "jdbc:postgresql://localhost/postchain",
                "database.username" to "postchain",
                "database.password" to "postchain",
                "database.schema" to "testschema"
        ))
    }
}