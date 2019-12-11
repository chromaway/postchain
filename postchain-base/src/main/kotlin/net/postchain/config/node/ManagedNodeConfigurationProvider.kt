package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.base.peerId
import net.postchain.config.DatabaseConnector
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer
import net.postchain.core.ByteArrayKey
import java.sql.Connection
import java.time.Instant

class ManagedNodeConfigurationProvider(
        appConfig: AppConfig,
        databaseConnector: (AppConfig) -> DatabaseConnector,
        appConfigDbLayer: (AppConfig, Connection) -> AppConfigDbLayer

) : ManualNodeConfigurationProvider(
        appConfig,
        databaseConnector,
        appConfigDbLayer
) {

    private var managedPeerSource: PeerInfoDataSource? = null

    fun setPeerInfoDataSource(peerInfoDataSource: PeerInfoDataSource?) {
        managedPeerSource = peerInfoDataSource
    }

    override fun getConfiguration(): NodeConfig {
        return object : NodeConfig(appConfig) {
            override val peerInfoMap = getPeerInfoMap(appConfig)
            override val nodeReplicas = managedPeerSource?.getNodeReplicaMap() ?: mapOf()
            override val blockchainReplicaNodes = managedPeerSource?.getBlockchainReplicaNodeMap() ?: mapOf()
        }
    }

    override fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        val peerInfoMap = mutableMapOf<ByteArrayKey, PeerInfo>()

        // Define pick function
        val peerInfoPicker: (PeerInfo) -> Unit = { peerInfo ->
            peerInfoMap.merge(peerInfo.peerId(), peerInfo) { old, new ->
                if (old.timestamp ?: Instant.EPOCH < new.timestamp ?: Instant.EPOCH) new else old
            }
        }

        // Collect peerInfos
        super.getPeerInfoCollection(appConfig).forEach(peerInfoPicker)
        managedPeerSource?.getPeerInfos()?.forEach(peerInfoPicker)

        return peerInfoMap.values.toTypedArray()
    }
}