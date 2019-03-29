package net.postchain.config.node

import net.postchain.config.app.AppConfig

object NodeConfigurationProviderFactory {

    fun create(appConfig: AppConfig): NodeConfigurationProvider {
        return when (appConfig.nodeConfigProvider) {
            "legacy" -> LegacyNodeConfigurationProvider(appConfig)
            "manual" -> ManualNodeConfigurationProvider(appConfig)
            "managed" -> ManagedNodeConfigurationProvider(appConfig)
            else -> LegacyNodeConfigurationProvider(appConfig)
        }
    }
}