package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.config.SimpleDatabaseConnector
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer

class ManualNodeConfigurationProvider(private val appConfig: AppConfig) : NodeConfigurationProvider {

    override fun getConfiguration(): NodeConfig {
        return object : NodeConfig(appConfig) {
            override val peerInfos = getPeerInfoCollection(appConfig)
        }
    }

    private fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        return SimpleDatabaseConnector(appConfig).withWriteConnection { connection ->
            AppConfigDbLayer(appConfig, connection).getPeerInfoCollection()
        }
    }
}