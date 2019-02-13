package net.postchain.network.x

import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo

interface PeersConnectionStrategy {
    fun forEach(configuration: PeerCommConfiguration, action: (PeerInfo) -> Unit)
}