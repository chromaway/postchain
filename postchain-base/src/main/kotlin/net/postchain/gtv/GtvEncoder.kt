// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv

import org.openmuc.jasn1.ber.ReverseByteArrayOutputStream;

/**
 * Responsible for turning GTV objects into binary data.
 */
object GtvEncoder {
    fun encodeGtv(v: Gtv): ByteArray {
        val outs = ReverseByteArrayOutputStream(1000, true)
        v.getRawGtv().encode(outs)
        return outs.array
    }
}