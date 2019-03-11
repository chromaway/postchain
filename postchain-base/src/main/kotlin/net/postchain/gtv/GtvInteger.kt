package net.postchain.gtv

import org.openmuc.jasn1.ber.types.BerInteger
import java.math.BigInteger
import net.postchain.gtv.messages.RawGtv

data class GtvInteger(val integer: BigInteger) : GtvPrimitive() {

    constructor(l: Long): this(BigInteger.valueOf(l))

    override val type: GtvType = GtvType.INTEGER

    override fun asInteger(): Long {
        return integer.toLong()
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
