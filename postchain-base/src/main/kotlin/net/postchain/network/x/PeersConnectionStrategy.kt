// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo

interface PeersConnectionStrategy {
    fun forEach(configuration: PeerCommConfiguration, action: (PeerInfo) -> Unit)
}