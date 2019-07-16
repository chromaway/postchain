package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.base.peerId
import net.postchain.config.SimpleDatabaseConnector
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer

class ManagedNodeConfigurationProvider(
        private val appConfig: AppConfig
) : ManualNodeConfigurationProvider(appConfig)
{
    private var managedPeerSource: PeerInfoDataSource? = null

    fun setPeerInfoDataSource(src: PeerInfoDataSource) {
        managedPeerSource = src
    }

    override fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        val c1 = super.getPeerInfoCollection(appConfig)
        if (managedPeerSource != null) {
            val c1Map = c1.associateBy { it.peerId() }
            val c2Map = managedPeerSource!!.getPeerInfos().associateBy { it.peerId() }

            return (c1Map + c2Map).values.toTypedArray()
        }
        else return c1
    }
}