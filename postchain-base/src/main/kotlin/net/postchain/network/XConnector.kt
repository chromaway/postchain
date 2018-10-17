package net.postchain.network

import net.postchain.base.PeerInfo
import net.postchain.core.ByteArrayKey

interface  XPeerConnection {
    fun close()
    fun accept(handler: XPacketHandler)
    fun sendPacket(packet: LazyPacket)
}

class XPeerConnectionDescriptor(
    val peerID: XPeerID,
    val blockchainRID: ByteArrayKey
)

interface XConnectorEvents {
    fun onPeerConnected(descriptor: XPeerConnectionDescriptor,
                        connection: XPeerConnection)
    fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor)
}

interface XConnector {
    fun connectPeer(descriptor: XPeerConnectionDescriptor)
}

class ActualXConnector(
        val myPeerInfo: PeerInfo,
        val identPacketConverter: IdentPacketConverter
): XConnector {
    override fun connectPeer(descriptor: XPeerConnectionDescriptor) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}