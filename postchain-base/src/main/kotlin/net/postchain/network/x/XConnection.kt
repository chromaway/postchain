package net.postchain.network.x

import net.postchain.base.PeerInfo
import net.postchain.network.IdentPacketConverter

interface XPeerConnection {
    fun close()
    fun accept(handler: XPacketHandler)
    fun sendPacket(packet: LazyPacket)
}

interface XConnectorEvents {
    fun onPeerConnected(descriptor: XPeerConnectionDescriptor,
                        connection: XPeerConnection)

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
