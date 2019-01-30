package net.postchain.network.x

import net.postchain.core.ByteArrayKey

class XPeerConnectionDescriptor(
        val peerId: XPeerID,
        val blockchainRID: ByteArrayKey,
        val sessionKey: ByteArray? = null
)