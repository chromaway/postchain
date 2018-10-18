package net.postchain.network.netty

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import net.postchain.base.PeerInfo
import net.postchain.network.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

class NettyPeerConnection(private val myPeerInfo: PeerInfo,
                          private val descriptor: XPeerConnectionDescriptor,
                          private val identPacketConverter: IdentPacketConverter): XPeerConnection {

    init {
        Thread { createClient() }.start()
    }

    private val packetSizeLength = 4

    private var ctx: ChannelHandlerContext? = null

    private val outboundPackets = LinkedBlockingQueue<ByteArray>(MAX_QUEUED_PACKETS)

    private var handler: XPacketHandler? = null

    override fun accept(handler: XPacketHandler) {
        this.handler = handler
    }

    override fun sendPacket(packet: LazyPacket) {
        if(ctx != null) {
            val message = packet.invoke()
            val packetSizeBytes = ByteBuffer.allocate(packetSizeLength).putInt(message.size).array()
            ctx!!.writeAndFlush(Unpooled.copiedBuffer(packetSizeBytes + message))
        }
    }

    override fun close() {
        group.shutdownGracefully().sync()
    }

    val group = NioEventLoopGroup()

    private fun createClient() {
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
        }
    }

    inner class ClientHandler : SimpleChannelInboundHandler<Any>() {

        override fun channelActive(channelHandlerContext: ChannelHandlerContext) {
            val identPacket = identPacketConverter.makeIdentPacket(myPeerInfo.pubKey)
            ctx = channelHandlerContext
            sendPacket({ identPacket })
        }

        override fun exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable) {
            channelHandlerContext.close()
        }

        override fun channelRead0(channelHandlerContext: ChannelHandlerContext, o: Any) {
            val bytes = readOnePacket(o)
            if (handler == null) {
                outboundPackets.put(bytes)
            } else {
                while (outboundPackets.isNotEmpty()) {
                    handler!!.invoke(outboundPackets.poll(), descriptor.peerID)
                }
                handler!!.invoke(bytes, descriptor.peerID)
            }
        }

        protected fun readOnePacket(msg: Any): ByteArray {
            val inBuffer = msg as ByteBuf
            val packetSizeHolder = ByteArray(packetSizeLength)
            inBuffer.readBytes(packetSizeHolder)
            val packetSize = ByteBuffer.wrap(packetSizeHolder).getInt()
            val bytes = ByteArray(packetSize)
            inBuffer.readBytes(bytes)
            return bytes
        }
    }
}