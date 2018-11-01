package net.postchain.network.netty

import io.netty.channel.nio.NioEventLoopGroup
import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.network.*
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnectionDescriptor

class NettyConnector(private val myPeerInfo: PeerInfo,
                     private val eventReceiver: XConnectorEvents,
                     private val identPacketConverter: IdentPacketConverter,
                     private val cryptoSystem: CryptoSystem): XConnector {

    val group = NioEventLoopGroup()
    init {
        NettyPassivePeerConnection(myPeerInfo, identPacketConverter, eventReceiver, group, cryptoSystem)
    }


    override fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo, eventReceiver: XConnectorEvents?) {
        NettyActivePeerConnection(peerInfo, myPeerInfo, descriptor, identPacketConverter, eventReceiver!!, group, cryptoSystem)
    }

    override fun shutdown() {
        group.shutdownGracefully().sync()
    }


}