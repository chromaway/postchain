// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.netty2

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

object Transport {

    fun wrapMessage(message: ByteArray): ByteBuf {
        return Unpooled.wrappedBuffer(message)
    }

    fun unwrapMessage(buffer: ByteBuf): ByteArray {
        return if (buffer.hasArray()) {
            buffer.array()
        } else {
            ByteArray(buffer.readableBytes()).also {
                buffer.getBytes(buffer.readerIndex(), it)
            }
        }
    }
}