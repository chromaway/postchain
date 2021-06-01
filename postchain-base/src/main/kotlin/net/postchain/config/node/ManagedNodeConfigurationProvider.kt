// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.node

import net.postchain.base.BlockchainRid
import net.postchain.base.PeerInfo
import net.postchain.base.Storage
import net.postchain.base.peerId
import net.postchain.config.app.AppConfig
import net.postchain.core.ByteArrayKey
import net.postchain.network.x.XPeerID
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
            // nodeReplicas: for making a node a full clone of another node
            override val nodeReplicas = managedPeerSource?.getNodeReplicaMap() ?: mapOf()
            override val blockchainReplicaNodes = getBlockchainReplicaCollection(appConfig)
            override val blockchainsToReplicate: Set<BlockchainRid> = getBlockchainsToReplicate(appConfig, pubKey)
            override val mustSyncUntilHeight = getSyncUntilHeight(appConfig)
        }
    }

    /**
     * This will collect PeerInfos from two sources:
     *
     * 1. The local peerinfos table (common for all blockchains)
     * 2. The chain0 c0.node table
     *
     * If there are multiple peerInfos for a specific key, the peerInfo
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

        // Collect peerInfos from local peerinfos table (common for all bcs)
        super.getPeerInfoCollection(appConfig).forEach(peerInfoPicker)
        // get the peerInfos from the chain0.node table
        managedPeerSource?.getPeerInfos()?.forEach(peerInfoPicker)

        return peerInfoMap.values.toTypedArray()
    }

    /**
     * This will collect BlockchainReplicas from two sources:
     *
     * 1. The local blockchain_replicas table (common for all blockchains)
     * 2. The chain0 c0.blockchain_replica_node table
     *
     */
    override fun getBlockchainReplicaCollection(appConfig: AppConfig): Map<BlockchainRid, List<XPeerID>> {
        // Collect from local table (common for all bcs)
        val localResMap = super.getBlockchainReplicaCollection(appConfig)
        // get values from the chain0 table
        val chain0ResMap = (managedPeerSource?.getBlockchainReplicaNodeMap() ?: mutableMapOf<BlockchainRid, List<XPeerID>>())

        val resMap = mutableMapOf<BlockchainRid, List<XPeerID>>()
        val allKeys = localResMap.keys + chain0ResMap.keys
        for (k in allKeys) {
            resMap[k] = merge(localResMap[k], chain0ResMap[k])
        }
        return resMap
    }

    fun merge(a: List<XPeerID>?, b: List<XPeerID>?): List<XPeerID> {
        if (a == null) {
            return b!!
        }
        if (b == null) {
            return a
        }
        return a.toSet().union(b).toList()
    }

    override fun getSyncUntilHeight(appConfig: AppConfig): Map<Long, Long> {
        //collect from local table: mapOf<chainID,height>
        val localMap = super.getSyncUntilHeight(appConfig)

        //collect from chain0 table. Mapped to brid instead of chainID, since chainID does not exist here. It is local.
        val bridToHeightMap = managedPeerSource?.getSyncUntilHeight() ?: mapOf()

        //brid2Height => chainID2height
        val bridToChainID = super.getChainIDs(appConfig)
        val c0Heights = mutableMapOf<Long, Long>()
        for (x in bridToHeightMap) {
            val chainIdKey = bridToChainID[x.key]
            c0Heights.put(chainIdKey!!, x.value)
        }

        // Primary source of height information is from local table, if not found there, use values from c0 tables.
        val resMap = mutableMapOf<Long, Long>()
        val allKeys = localMap.keys + c0Heights.keys
        for (k in allKeys) {
            resMap[k] = mergeLong(localMap[k], c0Heights[k])
        }
        return resMap
    }

    fun mergeLong(a: Long?, b: Long?): Long {
        if (a == null) {
            return b!!
        }
        if (b == null) {
            return a
        }
        return maxOf(a, b)
    }
}