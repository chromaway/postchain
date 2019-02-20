package net.postchain.gtv.merkle

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtv.GtvPath
import net.postchain.gtv.*
import net.postchain.gtv.GtvPathSet

/**
 * Used for the case when we are mixing dicts and arrays in the same test
 */
object MixArrayDictToGtvBinaryTreeHelper {



    const val expecedMerkleRoot_dict1_array4 = "09037170670903050505060305070508"

    fun buildGtvDictWithSubArray4(): GtvDictionary {
        // Add the inner GtvArray
        val innerIntArray = intArrayOf(1,2,3,4)
        val gtvArrayList =GtvTreeHelper.transformIntToGtv(innerIntArray.toCollection(ArrayList()))
        val gtvs: Array<Gtv> = gtvArrayList.toTypedArray()
        val innerGtvArr = GtvArray(gtvs)

        // Put the inner Array in the outer Dict
        val outerMap = HashMap<String, Gtv>()
        outerMap.set("one", innerGtvArr)
        return GtvDictionary(outerMap)
    }


}