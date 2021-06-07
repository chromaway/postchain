// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.netty2

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import mu.KLogging
import net.postchain.core.ProgrammerMistake
import net.postchain.network.XPacketDecoder
import net.postchain.network.x.LazyPacket
import net.postchain.network.x.XPacketHandler
import net.postchain.network.x.XPeerConnection
import net.postchain.network.x.XPeerConnectionDescriptor

class NettyServerPeerConnection<PacketType>(
        private val packetDecoder: XPacketDecoder<PacketType>
) : NettyPeerConnection() {

    private lateinit var context: ChannelHandlerContext
    private var packetHandler: XPacketHandler? = null
    private var peerConnectionDescriptor: XPeerConnectionDescriptor? = null

    private var onConnectedHandler: ((XPeerConnection) -> Unit)? = null
    private var onDisconnectedHandler: ((XPeerConnection) -> Unit)? = null

    companion object: KLogging()

    override fun accept(handler: XPacketHandler) {
        this.packetHandler = handler
    }

    override fun sendPacket(packet: LazyPacket) {
        context.writeAndFlush(Transport.wrapMessage(packet()))
    }

    override fun remoteAddress(): String {
        return if (::context.isInitialized)
            context.channel().remoteAddress().toString()
        else ""
    }

    override fun close() {
        context.close()
    }

    override fun descriptor(): XPeerConnectionDescriptor {
        return peerConnectionDescriptor ?: throw ProgrammerMistake("Descriptor is null")
    }

    fun onConnected(handler: (XPeerConnection) -> Unit): NettyServerPeerConnection<PacketType> {
        this.onConnectedHandler = handler
        return this
    }

    fun onDisconnected(handler: (XPeerConnection) -> Unit): NettyServerPeerConnection<PacketType> {
        this.onDisconnectedHandler = handler
        return this
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        handleSafely(peerConnectionDescriptor?.peerId) {
            val message = Transport.unwrapMessage(msg as ByteBuf)
            if (packetDecoder.isIdentPacket(message)) {
                val identPacketInfo = packetDecoder.parseIdentPacket(Transport.unwrapMessage(msg))
                peerConnectionDescriptor = XPeerConnectionDescriptor.createFromIdentPacketInfo(identPacketInfo)
                onConnectedHandler?.invoke(this)
            } else {
                if (peerConnectionDescriptor != null) {
                    packetHandler?.invoke(message, peerConnectionDescriptor!!.peerId)
                }
            }
            (msg as ByteBuf).release()
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        ctx?.let { context = it }
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        // If peerConnectionDescriptor is null, we can't do much handling
        // in which case we just ignore the inactivation of this channel.
        if (peerConnectionDescriptor != null) {
            onDisconnectedHandler?.invoke(this)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.debug("Error on connection.", cause)
    }
}
