package net.postchain.network.netty

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
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

class NettyActivePeerConnection(private val myPeerInfo: PeerInfo,
                                private val descriptor: XPeerConnectionDescriptor,
                                private val identPacketConverter: IdentPacketConverter,
                                private val eventReceiver: XConnectorEvents,
                                eventLoopGroup: EventLoopGroup): NettyIO(eventLoopGroup), XPeerConnection {

    private val outerThis = this

    override fun startSocket() {
        try {
            val clientBootstrap = Bootstrap()
            clientBootstrap.group(group)
            clientBootstrap.channel(NioSocketChannel::class.java)
            clientBootstrap.remoteAddress(InetSocketAddress(myPeerInfo.host, myPeerInfo.port))
            clientBootstrap.handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline()
                            .addLast(NettyIO.framePrepender)
                            .addLast(LengthFieldBasedFrameDecoder(MAX_PAYLOAD_SIZE, 0, packetSizeLength, 0, packetSizeLength))
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
            ctx = channelHandlerContext
            val identPacket = identPacketConverter.makeIdentPacket(sessionKeyHolder.getPublicKey())
            //sessionKeyHolder.initSessionKey() = String(identPacket.sliceArray(0 .. NettyIO.keySizeBytes))
            handler = eventReceiver.onPeerConnected(descriptor, outerThis)
            sendIdentPacket({ identPacket })

        }

        override fun exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable) {
            eventReceiver.onPeerDisconnected(descriptor)
            close()
        }

        @Volatile
        private var identified = false
        override fun channelRead0(channelHandlerContext: ChannelHandlerContext, o: Any) {
            if(!identified) {
                synchronized(identified) {
                    if (!identified) {
                        sessionKeyHolder.initSessionKey(readIdentPacket(o))
                        identified = true
                    } else {
                        handleInput(o)
                    }
                }
            } else {
                handleInput(o)
            }

        }
        private fun handleInput(o: Any) {
            if(handler != null) {
                val bytes = readOnePacket(o)
                if(bytes.isNotEmpty()) {
                    handler!!.invoke(bytes, descriptor.peerID)
                }
            } else {
                logger.error("${this::class.java.name}, handler is null")
            }
        }
    }
}