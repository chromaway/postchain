package net.postchain.network.netty

import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.network.IdentPacketConverter
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnectionDescriptor

class NettyConnector(private val myPeerInfo: PeerInfo,
                     private val eventReceiver: XConnectorEvents,
                     private val identPacketConverter: IdentPacketConverter,
                     private val cryptoSystem: CryptoSystem,
                     private val enabledEncryption: Boolean = true) : XConnector {

    val serverEventLoopGroup: EventLoopGroup
    val clientEventLoopGroup: EventLoopGroup
    val serverChannel: Class<ServerSocketChannel>
    val clientChannel: Class<SocketChannel>

    init {
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            serverEventLoopGroup = EpollEventLoopGroup()
            clientEventLoopGroup = EpollEventLoopGroup()
            serverChannel = EpollServerSocketChannel::class.java as Class<ServerSocketChannel>
            clientChannel = EpollSocketChannel::class.java as Class<SocketChannel>
        } else {
            serverEventLoopGroup = NioEventLoopGroup()
            clientEventLoopGroup = NioEventLoopGroup()
            serverChannel = NioServerSocketChannel::class.java as Class<ServerSocketChannel>
            clientChannel = NioSocketChannel::class.java as Class<SocketChannel>
        }
        NettyPassivePeerConnection(myPeerInfo, identPacketConverter, eventReceiver, serverChannel, serverEventLoopGroup, cryptoSystem, enabledEncryption)
    }

    override fun init(peerInfo: PeerInfo) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun connectPeer(descriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
        NettyActivePeerConnection(peerInfo, descriptor, identPacketConverter, eventReceiver, clientChannel, clientEventLoopGroup, cryptoSystem, enabledEncryption)
    }

    override fun shutdown() {
        serverEventLoopGroup.shutdownGracefully().sync()
        clientEventLoopGroup.shutdownGracefully().sync()
    }


}