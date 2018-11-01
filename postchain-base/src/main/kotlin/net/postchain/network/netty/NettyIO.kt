package net.postchain.network.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoopGroup
import io.netty.handler.codec.LengthFieldPrepender
import mu.KLogging
import net.postchain.base.CryptoSystem
import net.postchain.network.netty.bc.SymmetricEncryptorUtil
import net.postchain.network.x.LazyPacket
import net.postchain.network.x.XPacketHandler

/**
 * ruslan.klymenko@zorallabs.com 19.10.18
 */
abstract class NettyIO(protected val group: EventLoopGroup,
                       protected val cryptoSystem: CryptoSystem) {

    companion object : KLogging() {
        val packetSizeLength = 4
        val framePrepender = LengthFieldPrepender(packetSizeLength)
        val keySizeBytes = 32
    }

    var messagesSent = 0L
    protected set

    protected lateinit var handler: XPacketHandler

    protected lateinit var ctx: ChannelHandlerContext

    protected lateinit var sessionKey: ByteArray

    init {
        Thread({startSocket()}).start()
    }

    protected fun readEncryptedPacket(msg: Any): ByteArray {
        val bytes = readPacket(msg)
        return if(bytes.isEmpty()) bytes
               else SymmetricEncryptorUtil.decrypt(bytes, sessionKey!!)!!.byteArray
    }

    protected fun readPacket(msg: Any): ByteArray {
        val inBuffer = msg as ByteBuf
        val bytes = ByteArray(inBuffer.readableBytes())
        inBuffer.readBytes(bytes)
        return bytes
    }

    fun sendPacket(packet: LazyPacket) {
        if(ctx != null) {
            val message = SymmetricEncryptorUtil.encrypt(packet.invoke(), sessionKey!!, ++messagesSent)
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