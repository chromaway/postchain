package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.config.app.AppConfig

class ManagedNodeConfigurationProvider(
        private val appConfig: AppConfig
) : ManualNodeConfigurationProvider(appConfig) {

    private var managedPeerSource: PeerInfoDataSource? = null

    fun setPeerInfoDataSource(peerInfoDataSource: PeerInfoDataSource) {
        managedPeerSource = peerInfoDataSource
    }

    override fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        return super.getPeerInfoCollection(appConfig) +
                (managedPeerSource?.getPeerInfos() ?: emptyArray())
    }
}