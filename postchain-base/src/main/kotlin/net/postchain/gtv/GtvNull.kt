package net.postchain.gtv

import org.openmuc.jasn1.ber.types.BerNull
import net.postchain.gtv.messages.RawGtv

object GtvNull : GtvPrimitive() {

    override val type: GtvType = GtvType.NULL

    override fun isNull(): Boolean {
        return true
    }

    override fun getRawGtv(): RawGtv {
        return RawGtv(BerNull(), null, null, null, null, null)
    }

    override fun asPrimitive(): Any? {
        return null
    }

    override fun nrOfBytes(): Int {
        return 0
    }
}