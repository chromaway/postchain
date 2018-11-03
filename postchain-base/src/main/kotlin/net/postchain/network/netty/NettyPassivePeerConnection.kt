package net.postchain.network.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import net.postchain.base.CryptoSystem
import net.postchain.base.DynamicPortPeerInfo
import net.postchain.base.PeerInfo
import net.postchain.base.secp256k1_ecdh
import net.postchain.core.ByteArrayKey
import net.postchain.network.IdentPacketConverter
import net.postchain.network.IdentPacketInfo
import net.postchain.network.MAX_PAYLOAD_SIZE
import net.postchain.network.netty.bc.SymmetricEncryptorUtil
import net.postchain.network.x.*
import java.lang.Exception
import java.net.InetSocketAddress
import io.netty.channel.socket.*

/**
 * ruslan.klymenko@zorallabs.com 19.10.18
 */
class NettyPassivePeerConnection(private val peerInfo: PeerInfo,
                                 private val identPacketConverter: IdentPacketConverter,
                                 private val eventReceiver: XConnectorEvents,
                                 private val channelClass: Class<ServerSocketChannel>,
                                 eventLoopGroup: EventLoopGroup,
                                 cryptoSystem: CryptoSystem): NettyIO(eventLoopGroup, cryptoSystem) {

    override fun startSocket() {
        try {
            val serverBootstrap = ServerBootstrap()
            serverBootstrap.group(group)
            serverBootstrap.channel(channelClass)
            if(peerInfo is DynamicPortPeerInfo) {
                serverBootstrap.localAddress(InetSocketAddress(peerInfo.host, 0))
            } else {
                serverBootstrap.localAddress(InetSocketAddress(peerInfo.host, peerInfo.port))
            }
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

    inner class ServerHandler: ChannelInboundHandlerAdapter(), XPeerConnection {
        private lateinit var handler: XPacketHandler
        private lateinit var ctx: ChannelHandlerContext
        private lateinit var sessionKey: ByteArray
        private var connectionDescriptor: XPeerConnectionDescriptor? = null
        var messagesSent = 0L
        private set

        override fun sendPacket(packet: LazyPacket) {
            if(ctx != null) {
                val message = SymmetricEncryptorUtil.encrypt(packet.invoke(), sessionKey, ++messagesSent)
                ctx!!.writeAndFlush(Unpooled.wrappedBuffer(message), ctx!!.voidPromise())
            }
        }

        override fun close() {
            ctx?.close()
        }
        override fun accept(handler: XPacketHandler) {
            this.handler = handler
        }

        fun sendIdentPacket(packet: LazyPacket) {
        if(ctx != null) {
            ctx!!.writeAndFlush(Unpooled.wrappedBuffer(packet.invoke()), ctx!!.voidPromise())
        }
    }

        @Volatile
        private var receivedPassivePublicKey = false
        override fun channelRead(context: ChannelHandlerContext, msg: Any) {
            if(!receivedPassivePublicKey) {
                synchronized(receivedPassivePublicKey) {
                    if (!receivedPassivePublicKey) {
                        ctx = context
                        connectionDescriptor = getConnectionDescriptor(msg)
                        accept(eventReceiver.onPeerConnected(connectionDescriptor!!, this)!!)
                        sendIdentPacket { peerInfo.pubKey }
                        receivedPassivePublicKey = true
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
            val ecdh1 = secp256k1_ecdh(peerInfo.privateKey!!, peerInfo!!.pubKey!!)
            val ecdh2 = secp256k1_ecdh(peerInfo.privateKey!!, info.sessionKey!!)
            val digest = cryptoSystem.digest(ecdh1 + ecdh2)
            sessionKey = digest
        }
        fun readPacket(msg: Any) = NettyIO.readPacket(msg)

        fun readEncryptedPacket(msg: Any): ByteArray {
            val bytes = readPacket(msg)
            return if(bytes.isEmpty()) bytes
            else SymmetricEncryptorUtil.decrypt(bytes, sessionKey!!)!!.byteArray
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