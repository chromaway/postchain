package net.postchain.network.netty

import io.netty.buffer.ByteBuf
import io.netty.channel.EventLoopGroup
import io.netty.handler.codec.LengthFieldPrepender
import mu.KLogging
import net.postchain.base.CryptoSystem
import net.postchain.network.IdentPacketInfo
import net.postchain.network.x.XPeerConnectionDescriptor

/**
 * ruslan.klymenko@zorallabs.com 19.10.18
 */
abstract class NettyIO(protected val group: EventLoopGroup,
                       protected val cryptoSystem: CryptoSystem) {

    companion object : KLogging() {
        val packetSizeLength = 4
        val framePrepender = LengthFieldPrepender(packetSizeLength)
        val keySizeBytes = 32
        val identPacketDelimiter = "\\n".toByteArray()

        fun readPacket(msg: Any): ByteArray {
            val inBuffer = msg as ByteBuf
            val bytes = ByteArray(inBuffer.readableBytes())
            inBuffer.readBytes(bytes)
            return bytes
        }

        fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo {
            var lastStart = 0
            val result = mutableListOf<ByteArray>()
            bytes.forEachIndexed { indx, it ->
                if (indx != 0) {
                    if (bytes[indx - 1] == NettyIO.identPacketDelimiter[0] && it == NettyIO.identPacketDelimiter[1]) {
                        if (indx - 2 >= 0)
                            result.add(bytes.sliceArray(lastStart..indx - 2))
                        lastStart = indx + 1
                    }
                }
            }
            if (lastStart < bytes.size - 1) {
                result.add(bytes.sliceArray(lastStart..bytes.size - 1))
            }
            return IdentPacketInfo(result[0], result[1], result[2])
        }

        fun createIdentPacketBytes(descriptor: XPeerConnectionDescriptor, ephemeralPubKey: ByteArray) =
                descriptor.peerID.byteArray +
                        identPacketDelimiter +
                        descriptor.blockchainRID.byteArray +
                        identPacketDelimiter +
                        ephemeralPubKey!!
    }

    init {
        Thread({startSocket()}).start()
    }

    abstract fun startSocket()
}
class DecodedMessageHolder(val byteArray: ByteArray, val serial: Long)