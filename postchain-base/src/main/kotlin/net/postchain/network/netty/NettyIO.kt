package net.postchain.network.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import mu.KLogging
import net.postchain.network.MAX_PAYLOAD_SIZE
import net.postchain.network.x.LazyPacket
import net.postchain.network.x.XPacketHandler
import java.nio.ByteBuffer

/**
 * ruslan.klymenko@zorallabs.com 19.10.18
 */
abstract class NettyIO(protected val group: EventLoopGroup) {

    companion object : KLogging()

    protected val packetSizeLength = 4
    protected val frameDecoder = LengthFieldBasedFrameDecoder(MAX_PAYLOAD_SIZE, 0, packetSizeLength, 0, packetSizeLength)


    protected var handler: XPacketHandler? = null

    protected var ctx: ChannelHandlerContext? = null

    init {
        Thread({startSocket()}).start()
    }

    protected fun readOnePacket(msg: Any): ByteArray {
        val inBuffer = msg as ByteBuf
        val bytes = ByteArray(inBuffer.readableBytes())
        inBuffer.readBytes(bytes)
        return bytes
    }

    fun sendPacket(packet: LazyPacket) {
        if(ctx != null) {
            val message = packet.invoke()
            val packetSizeBytes = ByteBuffer.allocate(packetSizeLength).putInt(message.size).array()
            ctx!!.writeAndFlush(Unpooled.copiedBuffer(packetSizeBytes + message))
        }
    }

    fun close() {
        ctx?.close()
        group.shutdownGracefully().sync()
    }

    abstract fun startSocket()

    fun accept(handler: XPacketHandler) {
        this.handler = handler
    }
}