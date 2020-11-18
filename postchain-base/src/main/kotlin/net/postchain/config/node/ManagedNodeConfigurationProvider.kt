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
        createStorage: (AppConfig) -> Storage
) : ManualNodeConfigurationProvider(
        appConfig,
        createStorage
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

    /**
     * This will collect PeerInfos from two sources:
     *
     * 1. The global peerinfos table
     * 2. The chain0 c0.node table
     *
     * If there are multiple peerInfos for a specific key, tha peerInfo
     * with highest timestamp takes presedence. A null timestamp is considered
     * older than a non-null timestamp.
     *
     * The timestamp is taken directly from the respective table, c0.node_list
     * is not involved here.
     */
    override fun getPeerInfoCollection(appConfig: AppConfig): Array<PeerInfo> {
        val peerInfoMap = mutableMapOf<ByteArrayKey, PeerInfo>()

        // Define pick function
        val peerInfoPicker: (PeerInfo) -> Unit = { peerInfo ->
            peerInfoMap.merge(peerInfo.peerId(), peerInfo) { old, new ->
                if (old.timestamp ?: EPOCH < new.timestamp ?: EPOCH) new else old
            }
        }

        // Collect peerInfos from global peerinfos table
        super.getPeerInfoCollection(appConfig).forEach(peerInfoPicker)
        // get the peerInfos from the chain0.node table
        managedPeerSource?.getPeerInfos()?.forEach(peerInfoPicker)

        return peerInfoMap.values.toTypedArray()
    }
}