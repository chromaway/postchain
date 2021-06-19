package net.postchain.gtx.serializer

import net.postchain.base.data.DatabaseAccess
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvNull

class GtxEventDataSerializer {

    fun serializeToGtv(event: DatabaseAccess.EventInfo?): Gtv {
        if (event == null) return GtvNull
        return GtvFactory.gtv(GtvFactory.gtv(event.blockHeight), GtvFactory.gtv(event.pos), GtvFactory.gtv(event.hash), GtvFactory.gtv(event.data))
    }

}