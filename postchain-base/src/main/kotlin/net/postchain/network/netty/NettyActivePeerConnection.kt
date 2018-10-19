package net.postchain.network.netty

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import net.postchain.base.PeerInfo
import net.postchain.network.*
import net.postchain.network.x.XPeerConnection
import net.postchain.network.x.XPeerConnectionDescriptor
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue

class NettyActivePeerConnection(private val myPeerInfo: PeerInfo,
                                private val descriptor: XPeerConnectionDescriptor,
                                private val identPacketConverter: IdentPacketConverter): NettyIO(), XPeerConnection {

    private val outboundPackets = ConcurrentLinkedQueue<ByteArray>()

    override fun startSocket() {
        try {
            val clientBootstrap = Bootstrap()
            clientBootstrap.group(group)
            clientBootstrap.channel(NioSocketChannel::class.java)
            clientBootstrap.remoteAddress(InetSocketAddress(myPeerInfo.host, myPeerInfo.port))
            clientBootstrap.handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline()
                            .addLast(LengthFieldBasedFrameDecoder(MAX_PAYLOAD_SIZE, 0, packetSizeLength, 0, 0))
                            .addLast(ClientHandler())
                }
            })
            val channelFuture = clientBootstrap.connect().sync()
            channelFuture.channel().closeFuture().sync()
        } catch (e: Exception) {
            logger.error(e.toString())
        }
    }

    inner class ClientHandler : SimpleChannelInboundHandler<Any>() {

        override fun channelActive(channelHandlerContext: ChannelHandlerContext) {
            val identPacket = identPacketConverter.makeIdentPacket(myPeerInfo.pubKey)
            ctx = channelHandlerContext
            sendPacket({ identPacket })
        }

        override fun exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable) {
            close()
        }

        override fun channelRead0(channelHandlerContext: ChannelHandlerContext, o: Any) {
            val bytes = readOnePacket(o)
            if (handler == null) {
                outboundPackets.add(bytes)
            } else {
                while (outboundPackets.isNotEmpty()) {
                    handler!!.invoke(outboundPackets.poll(), descriptor.peerID)
                }
                handler!!.invoke(bytes, descriptor.peerID)
            }
        }
    }
}