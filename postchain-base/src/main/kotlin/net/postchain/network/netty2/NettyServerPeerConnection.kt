package net.postchain.network.netty2

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import net.postchain.base.PeerID
import net.postchain.core.byteArrayKeyOf
import net.postchain.network.IdentPacketInfo
import net.postchain.network.PacketConverter
import net.postchain.network.XPacketDecoder
import net.postchain.network.x.LazyPacket
import net.postchain.network.x.XPacketHandler
import net.postchain.network.x.XPeerConnection

class NettyServerPeerConnection<PacketType>(
        private val packetDecoder: XPacketDecoder<PacketType>
) : ChannelInboundHandlerAdapter(), XPeerConnection {

    private lateinit var context: ChannelHandlerContext
    private var packetHandler: XPacketHandler? = null
    private var peerId: PeerID? = null

    private var onConnectedHandler: ((XPeerConnection, IdentPacketInfo) -> Unit)? = null

    override fun accept(handler: XPacketHandler) {
        this.packetHandler = handler
    }

    override fun sendPacket(packet: LazyPacket) {
        context.writeAndFlush(Transport.wrapMessage(packet()))
    }

    override fun close() {
        context.close()
    }

    fun onConnected(handler: (XPeerConnection, IdentPacketInfo) -> Unit): ChannelHandler {
        this.onConnectedHandler = handler
        return this
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        val message = Transport.unwrapMessage(msg as ByteBuf)
        if (packetDecoder.isIdentPacket(message)) {
            val identPacketInfo = packetDecoder.parseIdentPacket(
                    Transport.unwrapMessage(msg))
            peerId = identPacketInfo.peerID
            onConnectedHandler?.invoke(this, identPacketInfo)

        } else {
            if (peerId != null) {
                packetHandler?.invoke(
                        message,
                        peerId!!.byteArrayKeyOf())
            }
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        ctx?.let { context = it }
    }
}
