package net.postchain.config.node

import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigProviders.*

object NodeConfigurationProviderFactory {

    fun createProvider(appConfig: AppConfig): NodeConfigurationProvider {
        return when (appConfig.nodeConfigProvider.toLowerCase()) {
            Legacy.name.toLowerCase() -> LegacyNodeConfigurationProvider(appConfig)
            Manual.name.toLowerCase() -> ManualNodeConfigurationProvider(appConfig)
            Managed.name.toLowerCase() -> ManagedNodeConfigurationProvider(appConfig)

            // TODO: Change 'Legacy' to 'Manual' in v3.0
            else -> LegacyNodeConfigurationProvider(appConfig)
        }
    }
}