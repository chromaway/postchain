package net.postchain.network.x

import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.core.Shutdownable
import net.postchain.network.PacketConverter

interface XPeerConnection {
    fun accept(handler: XPacketHandler)
    fun sendPacket(packet: LazyPacket)
    fun close()
}

interface XConnectorEvents {
    fun onPeerConnected(descriptor: XPeerConnectionDescriptor,
                        connection: XPeerConnection): XPacketHandler?

    fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor)
}

interface XConnector : Shutdownable {
    fun init(peerInfo: PeerInfo)
    // TODO: [et]: Two different structures for one thing
    fun connectPeer(peerConnectionDescriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo)
}

interface XConnectorFactory<PC : PacketConverter<*>> {
    fun createConnector(myPeerInfo: PeerInfo,
                        packetConverter: PC,
                        eventReceiver: XConnectorEvents,
                        cryptoSystem: CryptoSystem? = null): XConnector
}
