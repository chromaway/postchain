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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun shutdown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}