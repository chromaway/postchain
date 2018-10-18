package net.postchain.network.x

import net.postchain.core.ByteArrayKey

class XPeerConnectionDescriptor(
        val peerID: XPeerID,
        val blockchainRID: ByteArrayKey
)