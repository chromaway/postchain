package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.config.SimpleDatabaseConnector
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer

class ManagedNodeConfigurationProvider(
        private val appConfig: AppConfig
) : ManualNodeConfigurationProvider(appConfig)
{
    private lateinit var managedPeerSource: PeerInfoDataSource

    fun setPeerInfoDataSource(src: PeerInfoDataSource) {
        managedPeerSource = src
    }

    override fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        val c1 = super.getPeerInfoCollection(appConfig)
        if (::managedPeerSource.isInitialized) {
            return c1 + managedPeerSource.getPeerInfos()
        }
        else return c1
    }
}