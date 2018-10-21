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
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnection
import net.postchain.network.x.XPeerConnectionDescriptor
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue

class NettyActivePeerConnection(private val myPeerInfo: PeerInfo,
                                private val descriptor: XPeerConnectionDescriptor,
                                private val identPacketConverter: IdentPacketConverter,
                                private val eventReceiver: XConnectorEvents): NettyIO(), XPeerConnection {

    private val outerThis = this

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

        @Volatile
        private var identified = false
        override fun channelActive(channelHandlerContext: ChannelHandlerContext) {
            if(!identified) {
                synchronized(identified) {
                    if(!identified) {
                        ctx = channelHandlerContext
                        val identPacket = identPacketConverter.makeIdentPacket(myPeerInfo.pubKey)
                        handler = eventReceiver.onPeerConnected(descriptor, outerThis)
                        sendPacket({ identPacket })
                    }
                }
            } else {

            }
        }

        override fun exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable) {
            eventReceiver.onPeerDisconnected(descriptor)
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