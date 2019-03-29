package net.postchain.config.node

import net.postchain.config.app.AppConfig

class ManualNodeConfigurationProvider(private val appConfig: AppConfig) : NodeConfigurationProvider {

    override fun getConfiguration(): NodeConfig {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}