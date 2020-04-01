// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx.factory

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvString
import net.postchain.gtx.GtxBase
import net.postchain.gtx.OpData

object OpDataFactory {

    /**
     * Elements should be ordered like this:
     * 1. Operation name
     * 2. array of arguments
     */
    fun deserializeFromGtv(gtv: Gtv): OpData {
        // Check base structure
        val mainArr = gtv as GtvArray
        FactoryUtils.formatChecker(mainArr, GtxBase.NR_FIELDS_OPERATION, "OpData")

        // 1. Operation name
        val name: String = (mainArr[0] as GtvString).string

        // 2. array of arguments
        val args: Array<Gtv> = (mainArr[1] as GtvArray).array.map {it}.toTypedArray() // Not sure why this gets rid of "out"

        return OpData(name, args)
    }
}