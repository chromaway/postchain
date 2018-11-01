package net.postchain.network.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoopGroup
import io.netty.handler.codec.LengthFieldPrepender
import mu.KLogging
import net.postchain.network.netty.bc.SessionKeyHolder
import net.postchain.network.netty.bc.SymmetricEncryptorUtil
import net.postchain.network.x.LazyPacket
import net.postchain.network.x.XPacketHandler

/**
 * ruslan.klymenko@zorallabs.com 19.10.18
 */
abstract class NettyIO(protected val group: EventLoopGroup) {

    companion object : KLogging() {
        val packetSizeLength = 4
        val framePrepender = LengthFieldPrepender(packetSizeLength)
        val keySizeBytes = 32
    }

    protected var handler: XPacketHandler? = null

    protected var ctx: ChannelHandlerContext? = null

    protected var sessionKeyHolder = SessionKeyHolder(keySizeBytes)
    protected var messagesSent = 0L

    init {
        Thread({startSocket()}).start()
    }

    protected fun readOnePacket(msg: Any): ByteArray {
        val inBuffer = msg as ByteBuf
        val bytes = ByteArray(inBuffer.readableBytes())
        inBuffer.readBytes(bytes)

        return if(bytes.isEmpty()) bytes
               else SymmetricEncryptorUtil.decrypt(bytes, sessionKeyHolder.getSessionKey()!!)!!.byteArray
    }

    protected fun readIdentPacket(msg: Any): ByteArray {
        val inBuffer = msg as ByteBuf
        val bytes = ByteArray(inBuffer.readableBytes())
        inBuffer.readBytes(bytes)
        return bytes
    }

    fun sendPacket(packet: LazyPacket) {
        if(ctx != null) {
            val message = SymmetricEncryptorUtil.encrypt(packet.invoke(), sessionKeyHolder.getSessionKey()!!, ++messagesSent)
            ctx!!.writeAndFlush(Unpooled.wrappedBuffer(message), ctx!!.voidPromise())
        }
    }

    fun sendIdentPacket(packet: LazyPacket) {
        if(ctx != null) {
            ctx!!.writeAndFlush(Unpooled.wrappedBuffer(packet.invoke()), ctx!!.voidPromise())
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
class DecodedMessageHolder(val byteArray: ByteArray, val serial: Long)