package net.postchain.network

import net.postchain.base.PeerInfo
import net.postchain.core.ByteArrayKey
import net.postchain.network.x.LazyPacket
import net.postchain.network.x.XPacketHandler
import net.postchain.network.x.XPeerID

interface  XPeerConnection {
    fun close()
    fun sendPacket(packet: LazyPacket)
}

class XPeerConnectionDescriptor(
    val peerID: XPeerID,
    val blockchainRID: ByteArrayKey
)

interface XConnectorEvents {
    fun onPeerConnected(descriptor: XPeerConnectionDescriptor,
                        connection: XPeerConnection): XPacketHandler?
    fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor)
}

interface XConnector {
    fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo)
}

interface XConnectorFactory {
    fun createConnector(myPeerInfo: PeerInfo,
                        eventReceiver: XConnectorEvents,
                        identPacketConverter: IdentPacketConverter): XConnector
}

class ActualXConnector(
        val myPeerInfo: PeerInfo,
        val identPacketConverter: IdentPacketConverter,
        val eventReceiver: XConnectorEvents
): XConnector {
    override fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}