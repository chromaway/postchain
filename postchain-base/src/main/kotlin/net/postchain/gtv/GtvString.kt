package net.postchain.gtv

import net.postchain.common.hexStringToByteArray
import net.postchain.core.UserMistake
import net.postchain.gtv.messages.Gtv as RawGtv

data class GtvString(val string: String) : GtvPrimitive() {

    override val type: GtvType = GtvType.STRING

    override fun asString(): String {
        return string
    }

    override fun getRawGtv(): RawGtv {
        return RawGtv.string(string)
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
}