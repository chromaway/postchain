package net.postchain.network.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import net.postchain.network.MAX_PAYLOAD_SIZE
import java.lang.Exception
import java.net.InetSocketAddress

/**
 * ruslan.klymenko@zorallabs.com 02.11.18
 */
class NettyAcceptor {
//    override fun startSocket() {
//        try {
//            val serverBootstrap = ServerBootstrap()
//            serverBootstrap.group(group)
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