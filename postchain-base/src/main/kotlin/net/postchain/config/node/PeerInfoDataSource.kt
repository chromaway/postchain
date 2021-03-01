// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.node

import net.postchain.base.BlockchainRid
import net.postchain.base.PeerInfo
import net.postchain.network.x.XPeerID

interface PeerInfoDataSource {
    fun getPeerInfos(): Array<PeerInfo>
    fun getNodeReplicaMap(): Map<XPeerID, List<XPeerID>>
    fun getBlockchainReplicaNodeMap(): Map<BlockchainRid, List<XPeerID>>
    fun getSyncUntilHeight(): Map<BlockchainRid, Long>
}