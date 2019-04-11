package net.postchain.config.node

import net.postchain.config.app.AppConfig

class ManagedNodeConfigurationProvider(private val appConfig: AppConfig) : NodeConfigurationProvider {

    override fun getConfiguration(): NodeConfig {
        TODO("Communication with System Blockchain will be here")
    }
}