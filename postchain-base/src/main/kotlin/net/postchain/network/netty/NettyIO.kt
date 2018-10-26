package net.postchain.network.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoopGroup
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import mu.KLogging
import net.postchain.network.MAX_PAYLOAD_SIZE
import net.postchain.network.x.LazyPacket
import net.postchain.network.x.XPacketHandler

/**
 * ruslan.klymenko@zorallabs.com 19.10.18
 */
abstract class NettyIO(protected val group: EventLoopGroup) {

    companion object : KLogging() {
        val packetSizeLength = 4
        val framePrepender = LengthFieldPrepender(packetSizeLength)
    }

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
            ctx!!.writeAndFlush(Unpooled.wrappedBuffer(message), ctx!!.voidPromise())
        }
    }

    fun close() {
        ctx?.close()
    }

    abstract fun startSocket()

    fun accept(handler: XPacketHandler) {
        this.handler = handler
    }
}