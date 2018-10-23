package net.postchain.network.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.postchain.base.PeerInfo
import net.postchain.core.ByteArrayKey
import net.postchain.network.IdentPacketConverter
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnection
import net.postchain.network.x.XPeerConnectionDescriptor
import java.lang.Exception
import java.net.InetSocketAddress

/**
 * ruslan.klymenko@zorallabs.com 19.10.18
 */
class NettyPassivePeerConnection(private val peerInfo: PeerInfo,
                                 private val identPacketConverter: IdentPacketConverter,
                                 private val eventReceiver: XConnectorEvents,
                                 eventLoopGroup: EventLoopGroup): NettyIO(eventLoopGroup), XPeerConnection {

    private val outerThis = this

    private var connectionDescriptor: XPeerConnectionDescriptor? = null

    override fun startSocket() {
        try {
            val serverBootstrap = ServerBootstrap()
            serverBootstrap.group(group)
            serverBootstrap.channel(NioServerSocketChannel::class.java)
            serverBootstrap.localAddress(InetSocketAddress(peerInfo.host, peerInfo.port))
            val that = this
            serverBootstrap.childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline()
                            .addLast(frameDecoder)
                            .addLast(ServerHandler())
                }
            })
            val channelFuture = serverBootstrap.bind().sync()
            channelFuture.channel().closeFuture().sync()
        } catch (e: Exception) {
            logger.error(e.toString())
        }
    }

    inner class ServerHandler: ChannelInboundHandlerAdapter() {

        @Volatile
        private var identified = false
        override fun channelRead(context: ChannelHandlerContext, msg: Any) {
            if(!identified) {
                synchronized(identified) {
                    if (!identified) {
                        ctx = context
                        connectionDescriptor = getConnectionDescriptor(msg)
                        handler = eventReceiver.onPeerConnected(connectionDescriptor!!, outerThis)
                        identified = true
                    } else {
                        readAndHandleInput(msg)
                    }
                }
            } else {
                readAndHandleInput(msg)
            }
        }

        private fun getConnectionDescriptor(msg: Any): XPeerConnectionDescriptor {
            val info = identPacketConverter.parseIdentPacket(readOnePacket(msg))
            return XPeerConnectionDescriptor(ByteArrayKey(info.peerID), ByteArrayKey(info.blockchainRID))
        }

        private fun readAndHandleInput(msg: Any) {
            if(handler != null) {
                val bytes = readOnePacket(msg)
                handler!!.invoke(bytes, connectionDescriptor!!.peerID)
            } else {
                logger.error("${this::class.java.name}, handler is null")
            }
        }
        override fun channelReadComplete(ctx: ChannelHandlerContext) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
        }
        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            eventReceiver.onPeerDisconnected(connectionDescriptor!!)
            close()
        }
    }
}