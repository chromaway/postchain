package net.postchain.gtv

import net.postchain.common.hexStringToByteArray
import net.postchain.core.UserMistake
import org.openmuc.jasn1.ber.types.string.BerUTF8String
import net.postchain.gtv.messages.RawGtv

data class GtvString(val string: String) : GtvPrimitive() {

    override val type: GtvType = GtvType.STRING

    override fun asString(): String {
        return string
    }

    override fun getRawGtv(): RawGtv {
        return RawGtv(null, null, BerUTF8String(string), null, null, null)
    }

    override fun asByteArray(convert: Boolean): ByteArray {
        try {
            if (convert) {
                return string.hexStringToByteArray()
            } else return super.asByteArray(convert)
        } catch (e: Exception) {
            throw UserMistake("Can't create ByteArray from string '$string'")
        }
    }

    override fun asPrimitive(): Any? {
        return string
    }

    override fun nrOfBytes(): Int {
        return (string.length * 2)
    }
}