package net.postchain.config.node

import net.postchain.base.PeerInfo

interface PeerInfoDataSource {
    fun getPeerInfos(): Array<PeerInfo>
}