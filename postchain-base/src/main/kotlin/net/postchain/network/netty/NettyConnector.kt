package net.postchain.network.netty

import net.postchain.base.PeerInfo
import net.postchain.network.*
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnectionDescriptor
import kotlin.concurrent.thread

class NettyConnector(private val myPeerInfo: PeerInfo,
                     private val eventReceiver: XConnectorEvents,
                     private val identPacketConverter: IdentPacketConverter): XConnector {

    init {
        NettyPassivePeerConnection(myPeerInfo, identPacketConverter, eventReceiver).accept { data, _ -> println("Received server: ${String(data)}")}
    }


    override fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
        NettyActivePeerConnection(peerInfo, descriptor, identPacketConverter, eventReceiver).accept { data, _ -> println("Received client: ${String(data)}") }
    }


}