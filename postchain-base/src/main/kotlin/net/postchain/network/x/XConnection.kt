package net.postchain.network.x

import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.core.Shutdownable
import net.postchain.network.IdentPacketConverter

interface XPeerConnection {
    fun close()
    fun accept(handler: XPacketHandler)
    fun sendPacket(packet: LazyPacket)
}

interface XConnectorEvents {
    fun onPeerConnected(descriptor: XPeerConnectionDescriptor,
                        connection: XPeerConnection): XPacketHandler?

    fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor)
}

interface XConnector: Shutdownable {
    fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo, eventReceiver: XConnectorEvents?)
}

interface XConnectorFactory {
    fun createConnector(myPeerInfo: PeerInfo,
                        identPacketConverter: IdentPacketConverter,
                        eventReceiver: XConnectorEvents,
                        cryptoSystem: CryptoSystem): XConnector
}
