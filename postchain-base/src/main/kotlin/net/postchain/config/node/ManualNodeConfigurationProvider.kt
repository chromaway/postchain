package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.config.SimpleDatabaseConnector
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer
import net.postchain.network.x.XPeerID

open class ManualNodeConfigurationProvider(private val appConfig: AppConfig) : NodeConfigurationProvider {

    override fun getConfiguration(): NodeConfig {
        return object : NodeConfig(appConfig) {
            override val peerInfoMap = getPeerInfoMap(appConfig)
        }
    }

    private fun getPeerInfoMap(appConfig: AppConfig): Map<XPeerID, PeerInfo> =
            getPeerInfoCollection(appConfig).map { XPeerID(it.pubKey) to it }.toMap()

    open fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        return SimpleDatabaseConnector(appConfig).withWriteConnection { connection ->
            AppConfigDbLayer(appConfig, connection).getPeerInfoCollection()
        }
    }
}