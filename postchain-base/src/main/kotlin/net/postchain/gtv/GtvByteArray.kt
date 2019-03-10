package net.postchain.gtv

import java.util.*
import org.openmuc.jasn1.ber.types.BerOctetString
import net.postchain.gtv.messages.Gtv as RawGtv


data class GtvByteArray(val bytearray: ByteArray) : GtvPrimitive() {

    override val type: GtvType = GtvType.BYTEARRAY

    override fun asByteArray(convert: Boolean): ByteArray {
        return bytearray
    }

    override fun getRawGtv(): RawGtv {
        return RawGtv(null, BerOctetString(bytearray), null, null, null, null)
    }

    override fun asPrimitive(): Any? {
        return bytearray
    }

    override fun nrOfBytes(): Int {
        return bytearray.size
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GtvByteArray

        if (!Arrays.equals(bytearray, other.bytearray)) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(bytearray)
        result = 31 * result + type.hashCode()
        return result
    }
}