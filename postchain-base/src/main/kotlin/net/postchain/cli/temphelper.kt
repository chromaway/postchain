package net.postchain.cli

import net.postchain.StorageBuilder
import net.postchain.base.withWriteConnection
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.core.EContext
import net.postchain.core.NODE_ID_TODO
import org.apache.commons.configuration2.Configuration

// This code should be moved elsewhere

typealias DBCommandBody = (ctx: EContext, nodeConfig: Configuration) -> Unit

fun runDBCommandBody(nodeConfigFile: String, chainId: Long, body: DBCommandBody) {
    val nodeConfiguration = CommonsConfigurationFactory.readFromFile(nodeConfigFile)
    val storage = StorageBuilder.buildStorage(nodeConfiguration, NODE_ID_TODO)
    withWriteConnection(storage, chainId) {
        body(it, nodeConfiguration)
        true
    }
}
