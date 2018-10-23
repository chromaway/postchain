package net.postchain.network.netty

import net.postchain.base.PeerInfo
import net.postchain.network.IdentPacketConverter
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnectionDescriptor

class NettyConnector(private val myPeerInfo: PeerInfo,
                     private val eventReceiver: XConnectorEvents,
                     private val identPacketConverter: IdentPacketConverter) : XConnector {

    init {
        NettyPassivePeerConnection(myPeerInfo, identPacketConverter, eventReceiver)
    }

    override fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
        NettyActivePeerConnection(peerInfo, descriptor, identPacketConverter, eventReceiver)
    }

}