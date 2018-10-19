package net.postchain.network.netty

import net.postchain.base.PeerInfo
import net.postchain.network.*
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnectionDescriptor
import java.lang.Exception
import kotlin.concurrent.thread

class NettyConnector(private val myPeerInfo: PeerInfo,
                     private val eventReceiver: XConnectorEvents,
                     private val identPacketConverter: IdentPacketConverter): XConnector {

    init {
        thread{ NettyPassivePeerConnection(myPeerInfo, identPacketConverter, eventReceiver)}
    }


    override fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
        try {
            val connection = NettyActivePeerConnection(peerInfo, descriptor, identPacketConverter)
            eventReceiver.onPeerConnected(descriptor, connection)
        } catch(e: Exception) {
            eventReceiver.onPeerDisconnected(descriptor)
        }
    }


}