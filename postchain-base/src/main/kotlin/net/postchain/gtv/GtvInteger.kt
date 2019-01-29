package net.postchain.gtv

import net.postchain.gtv.messages.Gtv as RawGtv

data class GtvInteger(val integer: Long) : AbstractGtv() {

    override val type: GtvType = GtvType.INTEGER

    override fun asInteger(): Long {
        return integer
    }

    override fun getRawGtv(): RawGtv {
        return RawGtv.integer(integer)
    }

    override fun asPrimitive(): Any {
        return integer
    }

    override fun isContainerType() = false
}
