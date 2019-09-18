package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.base.peerId
import net.postchain.config.app.AppConfig

class ManagedNodeConfigurationProvider(
        private val appConfig: AppConfig
) : ManualNodeConfigurationProvider(appConfig) {

    private var managedPeerSource: PeerInfoDataSource? = null

    fun setPeerInfoDataSource(peerInfoDataSource: PeerInfoDataSource) {
        managedPeerSource = peerInfoDataSource
    }

    override fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        val c1 = super.getPeerInfoCollection(appConfig)
        return if (managedPeerSource != null) {
            val c1Map = c1.associateBy { it.peerId() }
            val c2Map = managedPeerSource!!.getPeerInfos().associateBy { it.peerId() }
            (c1Map + c2Map).values.toTypedArray()
        } else c1
    }
}