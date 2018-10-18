package net.postchain.network.netty

import net.postchain.base.PeerInfo
import net.postchain.network.*
import java.lang.Exception

class NettyConnector(private val eventReceiver: XConnectorEvents,
                     private val identPacketConverter: IdentPacketConverter): XConnector {

    private val connections = mutableListOf<XPeerConnection>()

    fun sendPacket(lazyPacket: LazyPacket) {
        connections.forEach {
            it.sendPacket(lazyPacket)
        }
    }

    override fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
        try {
            val connection = NettyPeerConnection(peerInfo, descriptor, identPacketConverter)
            connections.add(connection)
            eventReceiver.onPeerConnected(descriptor, connection)
        } catch(e: Exception) {
            eventReceiver.onPeerDisconnected(descriptor)
        }
    }
}