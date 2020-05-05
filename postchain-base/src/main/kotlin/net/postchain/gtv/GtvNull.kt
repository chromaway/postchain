// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv

import net.postchain.gtv.messages.RawGtv
import org.openmuc.jasn1.ber.types.BerNull

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