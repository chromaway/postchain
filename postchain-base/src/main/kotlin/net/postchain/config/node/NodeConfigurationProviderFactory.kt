package net.postchain.config.node

import net.postchain.config.DatabaseConnector
import net.postchain.config.SimpleDatabaseConnector
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer
import net.postchain.config.node.NodeConfigProviders.*
import java.sql.Connection

object NodeConfigurationProviderFactory {

    private val databaseConnectorFunction: (AppConfig) -> DatabaseConnector = { appConfig ->
        SimpleDatabaseConnector(appConfig)
    }

    private val appConfigDbLayerFunction: (AppConfig, Connection) -> AppConfigDbLayer = { appConfig, connection ->
        AppConfigDbLayer(appConfig, connection)
    }

    fun createProvider(appConfig: AppConfig): NodeConfigurationProvider {
        return when (appConfig.nodeConfigProvider.toLowerCase()) {
            Legacy.name.toLowerCase() -> LegacyNodeConfigurationProvider(appConfig)

            Manual.name.toLowerCase() -> ManualNodeConfigurationProvider(
                    appConfig, databaseConnectorFunction, appConfigDbLayerFunction)

            Managed.name.toLowerCase() -> ManagedNodeConfigurationProvider(
                    appConfig, databaseConnectorFunction, appConfigDbLayerFunction)

            // TODO: Change 'Legacy' to 'Manual' in v3.0
            else -> LegacyNodeConfigurationProvider(appConfig)
        }
    }
}