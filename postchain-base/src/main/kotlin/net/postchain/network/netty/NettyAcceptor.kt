package net.postchain.network.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.network.IdentPacketConverter
import net.postchain.network.MAX_PAYLOAD_SIZE
import net.postchain.network.x.XConnectorEvents
import java.lang.Exception
import java.net.InetSocketAddress

/**
 * ruslan.klymenko@zorallabs.com 02.11.18
 */
class NettyAcceptor(private val peerInfo: PeerInfo,
                    private val identPacketConverter: IdentPacketConverter,
                    private val eventReceiver: XConnectorEvents,
                    private val eventLoopGroup: EventLoopGroup,
                    cryptoSystem: CryptoSystem) {
//    override fun startSocket() {
//        try {
//            val serverBootstrap = ServerBootstrap()
//            serverBootstrap.group(eventLoopGroup)
//            serverBootstrap.channel(NioServerSocketChannel::class.java)
//            serverBootstrap.localAddress(InetSocketAddress(peerInfo.host, 0))
//            serverBootstrap.childHandler(object : ChannelInitializer<SocketChannel>() {
//                override fun initChannel(socketChannel: SocketChannel) {
//                    socketChannel.pipeline()
//                            .addLast(NettyIO.framePrepender)
//                            .addLast(LengthFieldBasedFrameDecoder(MAX_PAYLOAD_SIZE, 0, NettyIO.packetSizeLength, 0, NettyIO.packetSizeLength))
//                            .addLast(ServerHandler())
//                }
//            })
//            val channelFuture = serverBootstrap.bind().sync()
//            channelFuture.channel().closeFuture().sync()
//        } catch (e: Exception) {
//            NettyIO.logger.error(e.toString())
//        }
//    }
}