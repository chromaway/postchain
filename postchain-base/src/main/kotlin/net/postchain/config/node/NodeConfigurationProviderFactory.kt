// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.node

import net.postchain.StorageBuilder
import net.postchain.base.Storage
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigProviders.*
import net.postchain.core.NODE_ID_NA

object NodeConfigurationProviderFactory {

    private val storageSupplier: (AppConfig) -> Storage = { appConfig ->
        StorageBuilder.buildStorage(appConfig, NODE_ID_NA)
    }

    fun createProvider(appConfig: AppConfig): NodeConfigurationProvider {
        return when (appConfig.nodeConfigProvider.toLowerCase()) {
            Legacy.name.toLowerCase() -> LegacyNodeConfigurationProvider(appConfig)

            Manual.name.toLowerCase() -> ManualNodeConfigurationProvider(
                    appConfig, storageSupplier)

            Managed.name.toLowerCase() -> ManagedNodeConfigurationProvider(
                    appConfig, storageSupplier)

            // TODO: Change 'Legacy' to 'Manual' in v3.0
            else -> LegacyNodeConfigurationProvider(appConfig)
        }
    }
}