package net.postchain.gtv

import net.postchain.gtv.messages.Gtv as RawGtv

data class GtvBoolean(val bool: Boolean) : GtvPrimitive() {

    override val type: GtvType = GtvType.BOOLEAN

    fun Boolean.toInt() = if (this) 1L else 0L

    override fun asInteger(): Long {
        return bool.toInt()
    }
    override fun asBoolean(): Boolean {
        return bool
    }

    override fun getRawGtv(): RawGtv {
        return RawGtv.integer(bool.toInt())
    }

    override fun asPrimitive(): Any {
        return bool
    }

    override fun nrOfBytes(): Int {
        return 1
    }
}