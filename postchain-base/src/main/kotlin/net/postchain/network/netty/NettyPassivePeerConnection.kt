package net.postchain.network.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.base.secp256k1_ecdh
import net.postchain.core.ByteArrayKey
import net.postchain.network.IdentPacketConverter
import net.postchain.network.IdentPacketInfo
import net.postchain.network.MAX_PAYLOAD_SIZE
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
                                 eventLoopGroup: EventLoopGroup,
                                 cryptoSystem: CryptoSystem): NettyIO(eventLoopGroup, cryptoSystem), XPeerConnection {

    private val outerThis = this

    private var connectionDescriptor: XPeerConnectionDescriptor? = null

    override fun startSocket() {
        try {
            val serverBootstrap = ServerBootstrap()
            serverBootstrap.group(group)
            serverBootstrap.channel(NioServerSocketChannel::class.java)
            serverBootstrap.localAddress(InetSocketAddress(peerInfo.host, peerInfo.port))
            serverBootstrap.childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline()
                            .addLast(NettyIO.framePrepender)
                            .addLast(LengthFieldBasedFrameDecoder(MAX_PAYLOAD_SIZE, 0, packetSizeLength, 0, packetSizeLength))
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
        private var receivedActiveConnectionDescriptor = false
        override fun channelRead(context: ChannelHandlerContext, msg: Any) {
            if(!receivedActiveConnectionDescriptor) {
                synchronized(receivedActiveConnectionDescriptor) {
                    if (!receivedActiveConnectionDescriptor) {
                        ctx = context
                        connectionDescriptor = getConnectionDescriptor(msg)
                        accept(eventReceiver.onPeerConnected(connectionDescriptor!!, outerThis)!!)
                        sendIdentPacket { peerInfo.pubKey }
                        receivedActiveConnectionDescriptor = true
                    } else {
                        readAndHandleInput(msg)
                    }
                }
            } else {
                readAndHandleInput(msg)
            }
        }

        private fun getConnectionDescriptor(msg: Any): XPeerConnectionDescriptor {
            val info = identPacketConverter.parseIdentPacket(readPacket(msg))
            generateSessionKey(info)
            return XPeerConnectionDescriptor(ByteArrayKey(info.peerID), ByteArrayKey(info.blockchainRID), info.sessionKey!!)
        }

        private fun generateSessionKey(info: IdentPacketInfo) {
            val ecdh1 = secp256k1_ecdh(peerInfo.privateKey!!, info!!.sessionKey!!)
            val ecdh2 = secp256k1_ecdh(peerInfo.privateKey!!, info.ephemeralPubKey!!)
            val digest = cryptoSystem.digest(ecdh1 + ecdh2)
            sessionKey = digest
        }

        private fun readAndHandleInput(msg: Any) {
            if(handler != null) {
                val bytes = readEncryptedPacket(msg)
                handler!!.invoke(bytes, connectionDescriptor!!.peerID)
            } else {
                logger.error("${this::class.java.name}, handler is null")
            }
        }
        override fun channelReadComplete(ctx: ChannelHandlerContext) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER, ctx.voidPromise())
        }
        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            eventReceiver.onPeerDisconnected(connectionDescriptor!!)
            close()
        }
    }
}