// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx.factory

import net.postchain.gtv.GtvArray

object FactoryUtils {

    /**
     * Validates the number of fields of the GTX object we need.
     */
    fun formatChecker(gtvArr: GtvArray, size: Int, type: String) {
        if (gtvArr.getSize() < size)
        {
            throw IllegalArgumentException("Incorrect format for $type ,array size: {${gtvArr.getSize()}")
        }
    }
}