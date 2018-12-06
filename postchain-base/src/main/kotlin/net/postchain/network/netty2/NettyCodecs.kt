package net.postchain.network.netty2

import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import net.postchain.network.MAX_PAYLOAD_SIZE

object NettyCodecs {

    fun lengthFieldPrepender(): LengthFieldPrepender {
        return LengthFieldPrepender(4)
    }

    fun lengthFieldBasedFrameDecoder(): LengthFieldBasedFrameDecoder {
        return LengthFieldBasedFrameDecoder(
                MAX_PAYLOAD_SIZE,
                0,
                4,
                0,
                4)
    }
}