package net.postchain.network.x

import net.postchain.base.PeerInfo
import net.postchain.core.Shutdownable
import net.postchain.network.IdentPacketConverter

class DefaultXConnector(
        val myPeerInfo: PeerInfo,
        val identPacketConverter: IdentPacketConverter,
        val eventReceiver: XConnectorEvents
) : XConnector, Shutdownable {

    override fun init(peerInfo: PeerInfo) {
    }

    override fun shutdown() {
    }

    override fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
    }
}