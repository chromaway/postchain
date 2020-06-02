// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.base.Storage
import net.postchain.base.peerId
import net.postchain.config.app.AppConfig
import net.postchain.core.ByteArrayKey
import java.time.Instant.EPOCH

class ManagedNodeConfigurationProvider(
        appConfig: AppConfig,
        storageSupplier: (AppConfig) -> Storage
) : ManualNodeConfigurationProvider(
        appConfig,
        storageSupplier
) {

    private var managedPeerSource: PeerInfoDataSource? = null

    fun setPeerInfoDataSource(peerInfoDataSource: PeerInfoDataSource?) {
        managedPeerSource = peerInfoDataSource
    }

    override fun getConfiguration(): NodeConfig {
        return object : NodeConfig(appConfig) {
            override val peerInfoMap = getPeerInfoCollection(appConfig)
                    .associateBy(PeerInfo::peerId)
            override val nodeReplicas = managedPeerSource?.getNodeReplicaMap() ?: mapOf()
            override val blockchainReplicaNodes = managedPeerSource?.getBlockchainReplicaNodeMap() ?: mapOf()
        }
    }

    // TODO: [et]: Make it protected
    override fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        val peerInfoMap = mutableMapOf<ByteArrayKey, PeerInfo>()

        // Define pick function
        val peerInfoPicker: (PeerInfo) -> Unit = { peerInfo ->
            peerInfoMap.merge(peerInfo.peerId(), peerInfo) { old, new ->
                if (old.timestamp ?: EPOCH < new.timestamp ?: EPOCH) new else old
            }
        }

        // Collect peerInfos
        super.getPeerInfoCollection(appConfig).forEach(peerInfoPicker)
        managedPeerSource?.getPeerInfos()?.forEach(peerInfoPicker)

        return peerInfoMap.values.toTypedArray()
    }
}