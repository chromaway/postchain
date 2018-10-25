package net.postchain.network.netty

import io.netty.channel.nio.NioEventLoopGroup
import net.postchain.base.PeerInfo
import net.postchain.network.*
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnectionDescriptor

class NettyConnector(private val myPeerInfo: PeerInfo,
                     private val eventReceiver: XConnectorEvents,
                     private val identPacketConverter: IdentPacketConverter): XConnector {

    val group = NioEventLoopGroup()
    init {
        NettyPassivePeerConnection(myPeerInfo, identPacketConverter, eventReceiver, group)
    }


    override fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
        NettyActivePeerConnection(peerInfo, descriptor, identPacketConverter, eventReceiver, group)
    }

    override fun shutdown() {
        group.shutdownGracefully().sync()
    }


}