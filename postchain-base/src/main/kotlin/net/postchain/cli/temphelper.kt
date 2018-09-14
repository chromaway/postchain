package net.postchain.cli

import net.postchain.base.withWriteConnection
import net.postchain.baseStorage
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.core.EContext
import org.apache.commons.configuration2.Configuration

// This code should be moved elsewhere

typealias DBCommandBody = (ctx: EContext, nodeConfig: Configuration) -> Unit

fun runDBCommandBody(nodeConfigFile: String, chainId: Long, body: DBCommandBody) {
    val nodeConfiguration = CommonsConfigurationFactory.readFromFile(nodeConfigFile)
    val storage = baseStorage(nodeConfiguration, -1, null /*Will be eliminate later*/)
    withWriteConnection(storage, chainId) {
        body(it, nodeConfiguration)
        true
    }
}
