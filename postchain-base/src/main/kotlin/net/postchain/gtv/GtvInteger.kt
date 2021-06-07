// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv

import net.postchain.gtv.messages.RawGtv
import org.openmuc.jasn1.ber.types.BerInteger
import java.math.BigInteger

data class GtvInteger(val integer: BigInteger) : GtvPrimitive() {

    constructor(l: Long): this(BigInteger.valueOf(l))

    override val type: GtvType = GtvType.INTEGER

    override fun asInteger(): Long {
        return integer.longValueExact()
    }

    override fun asBigInteger(): BigInteger {
        return integer
    }

    override fun asBoolean(): Boolean {
        return integer.toLong().toBoolean()
    }

    override fun getRawGtv(): RawGtv {
        return RawGtv(null, null, null, BerInteger(integer), null, null)
    }

    override fun asPrimitive(): Any {
        return integer
    }

    override fun nrOfBytes(): Int {
        return ((integer.bitLength() + 1) / 8) + 1
    }
}
