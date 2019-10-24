package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.config.DatabaseConnector
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer
import net.postchain.network.x.XPeerID
import java.sql.Connection

open class ManualNodeConfigurationProvider(
        private val appConfig: AppConfig,
        private val databaseConnector: (AppConfig) -> DatabaseConnector,
        private val appConfigDbLayer: (AppConfig, Connection) -> AppConfigDbLayer
) : NodeConfigurationProvider {

    override fun getConfiguration(): NodeConfig {
        return object : NodeConfig(appConfig) {
            override val peerInfoMap = getPeerInfoMap(appConfig)
        }
    }

    private fun getPeerInfoMap(appConfig: AppConfig): Map<XPeerID, PeerInfo> =
            getPeerInfoCollection(appConfig).map { XPeerID(it.pubKey) to it }.toMap()

    open fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        return databaseConnector(appConfig).withWriteConnection { connection ->
            appConfigDbLayer(appConfig, connection).getPeerInfoCollection()
        }
    }
}