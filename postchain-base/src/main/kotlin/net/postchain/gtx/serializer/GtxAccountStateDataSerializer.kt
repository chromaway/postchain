package net.postchain.gtx.serializer

import net.postchain.base.data.DatabaseAccess
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvNull

class GtxAccountStateDataSerializer {

    fun serializeToGtv(state: DatabaseAccess.AccountState?): Gtv {
        if (state == null) return GtvNull
        return GtvFactory.gtv(GtvFactory.gtv(state.blockHeight), GtvFactory.gtv(state.stateN), GtvFactory.gtv(state.data))
    }

}