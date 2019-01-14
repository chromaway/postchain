package net.postchain.network.netty2

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import net.postchain.base.PeerInfo
import net.postchain.base.peerId
import net.postchain.network.IdentPacketConverter
import net.postchain.network.x.LazyPacket
import net.postchain.network.x.XPacketHandler
import net.postchain.network.x.XPeerConnection
import nl.komponents.kovenant.task
import java.net.InetSocketAddress
import java.net.SocketAddress

class NettyClientPeerConnection(
        val peerInfo: PeerInfo,
        val identPacketConverter: IdentPacketConverter
) : ChannelInboundHandlerAdapter(), XPeerConnection {

    private val nettyClient = NettyClient()
    private lateinit var context: ChannelHandlerContext
    private var packetHandler: XPacketHandler? = null
    private lateinit var onDisconnected: () -> Unit

    fun open(onConnected: () -> Unit, onDisconnected: () -> Unit) {
        this.onDisconnected = onDisconnected

        nettyClient.apply {
            setChannelHandler(this@NettyClientPeerConnection)
            connect(peerAddress())
            if (connectFuture.isSuccess) {
                onConnected()
            }
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        ctx?.let {
            context = ctx
            context.writeAndFlush(buildIdentPacket())
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        onDisconnected()
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        packetHandler?.invoke(
                Transport.unwrapMessage(msg as ByteBuf),
                peerInfo.peerId())
    }

    override fun accept(handler: XPacketHandler) {
        packetHandler = handler
    }

    override fun sendPacket(packet: LazyPacket) {
        context.writeAndFlush(Transport.wrapMessage(packet()))
    }

    override fun close() {
        task {
            nettyClient.shutdown()
        }
    }

    private fun peerAddress(): SocketAddress {
        return InetSocketAddress(peerInfo.host, peerInfo.port)
    }

    private fun buildIdentPacket(): ByteBuf {
        return Transport.wrapMessage(
                identPacketConverter.makeIdentPacket(peerInfo.pubKey))
    }
}