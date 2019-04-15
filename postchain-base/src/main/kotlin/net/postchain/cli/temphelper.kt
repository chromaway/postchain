package net.postchain.cli

import net.postchain.StorageBuilder
import net.postchain.base.withWriteConnection
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.core.EContext
import net.postchain.core.NODE_ID_TODO

// This code should be moved elsewhere

typealias DBCommandBody = (ctx: EContext) -> Unit

fun runDBCommandBody(nodeConfigFile: String, chainId: Long, body: DBCommandBody) {
    val nodeConfig = NodeConfigurationProviderFactory.createProvider(
            AppConfig.fromPropertiesFile(nodeConfigFile)
    ).getConfiguration()

    val storage = StorageBuilder.buildStorage(nodeConfig, NODE_ID_TODO)
    withWriteConnection(storage, chainId) {
        body(it)
        true
    }
    storage.close()
}
