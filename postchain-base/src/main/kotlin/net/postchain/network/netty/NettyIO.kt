package net.postchain.network.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.nio.NioEventLoopGroup
import mu.KLogging
import net.postchain.network.x.LazyPacket
import net.postchain.network.x.XPacketHandler
import java.nio.ByteBuffer

/**
 * ruslan.klymenko@zorallabs.com 19.10.18
 */
object NettyGroupHolder {
    val group = NioEventLoopGroup()
}
abstract class NettyIO {

    companion object : KLogging()

    protected var handler: XPacketHandler? = null

    protected val packetSizeLength = 4
    protected var ctx: ChannelHandlerContext? = null

    protected val group = NettyGroupHolder.group


    init {
        Thread({startSocket()}).start()
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