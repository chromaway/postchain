// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import net.postchain.StorageBuilder
import net.postchain.base.withWriteConnection
import net.postchain.config.app.AppConfig
import net.postchain.core.EContext
import net.postchain.core.NODE_ID_TODO

// This code should be moved elsewhere

typealias DBCommandBody = (ctx: EContext) -> Unit

fun runDBCommandBody(nodeConfigFile: String, chainId: Long, body: DBCommandBody) {
    val appConfig = AppConfig.fromPropertiesFile(nodeConfigFile)
    val storage = StorageBuilder.buildStorage(appConfig, NODE_ID_TODO)
    withWriteConnection(storage, chainId) {
        body(it)
        true
    }
    storage.close()
}
