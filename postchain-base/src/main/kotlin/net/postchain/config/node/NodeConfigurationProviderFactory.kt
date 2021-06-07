// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.node

import net.postchain.StorageBuilder
import net.postchain.base.Storage
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigProviders.*
import net.postchain.core.NODE_ID_NA

object NodeConfigurationProviderFactory {

    val DEFAULT_STORAGE_FACTORY: (AppConfig) -> Storage = {
        StorageBuilder.buildStorage(it, NODE_ID_NA)
    }

    fun createProvider(
            appConfig: AppConfig,
            storageFactory: (AppConfig) -> Storage = DEFAULT_STORAGE_FACTORY
    ): NodeConfigurationProvider {

        return when (appConfig.nodeConfigProvider.toLowerCase()) {
            Legacy.name.toLowerCase() -> LegacyNodeConfigurationProvider(appConfig)
            Manual.name.toLowerCase() -> ManualNodeConfigurationProvider(appConfig, storageFactory)
            Managed.name.toLowerCase() -> ManagedNodeConfigurationProvider(appConfig, storageFactory)
            // TODO: Change 'Legacy' to 'Manual' in v3.0
            else -> LegacyNodeConfigurationProvider(appConfig)
        }
    }
}