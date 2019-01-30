package net.postchain.gtv

import net.postchain.gtv.messages.Gtv as RawGtv

object GtvNull : GtvPrimitive() {

    override val type: GtvType = GtvType.NULL

    override fun isNull(): Boolean {
        return true
    }

    override fun getRawGtv(): net.postchain.gtv.messages.Gtv {
        return RawGtv.null_(null)
    }

    override fun asPrimitive(): Any? {
        return null
    }
}