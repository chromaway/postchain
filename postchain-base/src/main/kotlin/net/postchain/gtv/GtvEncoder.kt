package net.postchain.gtv

import java.io.ByteArrayOutputStream

/**
 * Responsible for turning GTV objects into binary data.
 */
object GtvEncoder {

    fun encodeGtv(v: Gtv): ByteArray {
        val outs = ByteArrayOutputStream()
        v.getRawGtv().der_encode(outs)
        return outs.toByteArray()
    }
}