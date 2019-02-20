package net.postchain.gtv.merkle

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvPath
import net.postchain.gtv.GtvPathSet
import net.postchain.gtv.Gtv

object DictToGtvBinaryTreeHelper {

    const val expectedMerkleRoot1 = "09037170670303"
    const val expectedMerkleRoot4 = "090203056A737976050803057372690505020305786C76696905070305787B730506"
    const val expectedMerkleRootDictInDict = "09037170670A0305696D6B6C78050C030577697A6972050B"

    // ----------------- 1 -----------------------------
    /**
     * When we only have one element in the Dict we don't have to generate dummies, since a dict will always have even pairs.
     */
    fun buildGtvDictOf1(): GtvDictionary {
        val stringArray = arrayOf("one")
        val intArray = intArrayOf(1)
        return GtvTreeHelper.transformStringAndIntToGtvDictionary(stringArray.toCollection(ArrayList()), intArray.toCollection(ArrayList()))
    }

    // ----------------- 4 -----------------------------
    fun buildGtvDictOf4(): GtvDictionary {
        val stringArray = arrayOf("one","two","three","four")
        val intArray = intArrayOf(1,2,3,4)
        return GtvTreeHelper.transformStringAndIntToGtvDictionary(stringArray.toCollection(ArrayList()), intArray.toCollection(ArrayList()))
    }

    // ----------------- 1 and 2 -----------------------------
    /**
     * Dict within a dict
     */
    fun buildGtvDictOf1WithSubDictOf2(): GtvDictionary {
        // Add the inner GtvDictionary
        val innerStringArray = arrayOf("seven", "eight")
        val innerIntArray = intArrayOf(7, 8)
        val innerGtvDict =GtvTreeHelper.transformStringAndIntToGtvDictionary(innerStringArray.toCollection(ArrayList()), innerIntArray.toCollection(ArrayList()))

        // Put the inner Dict in the outer Dict
        val outerMap = HashMap<String, Gtv>()
        outerMap.set("one", innerGtvDict)
        return GtvDictionary(outerMap)
    }


}