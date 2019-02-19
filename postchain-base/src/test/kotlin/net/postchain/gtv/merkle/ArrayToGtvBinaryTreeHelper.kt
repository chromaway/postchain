package net.postchain.gtv.merkle

import net.postchain.gtv.GtvArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv

object ArrayToGtvBinaryTreeHelper {

    const val expected1ElementArrayMerkleRoot = "070203010101010101010101010101010101010101010101010101010101010101010101"
    const val expected4ElementArrayMerkleRoot = "0701030403050103060307"
    const val expected7ElementArrayMerkleRoot = "07010204050406020407040801020409040A030A"
    const val expectet7and3ElementArrayMerkleRoot = "070102040504060204070A040607060F050801020409040A030A"

    // ----------------- 1 -----------------------------
    fun intArrOf1() = intArrayOf(1)

    fun buildGtvArrayOf1(): GtvArray {
        val intArray = intArrOf1()
        val gtvArrayList = GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))
        return gtv(gtvArrayList)
    }

    // ----------------- 4 -----------------------------
    fun intArrOf4() = intArrayOf(1,2,3,4)

    fun buildGtvArrayOf4(): GtvArray {
        val intArray = intArrOf4()
        val gtvArrayList =GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))
        return gtv(gtvArrayList)
    }

    // ----------------- 7 -----------------------------
    fun intArrOf7() = intArrayOf(1, 2, 3, 4, 5, 6, 7)

    fun buildGtvArrayOf7(): GtvArray {
        val intArray = intArrOf7()
        val gtvList: List<Gtv> =GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))
        return gtv(gtvList)
    }

    // ----------------- 9 -----------------------------
    fun intArrOf9() = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

    fun buildGtvArrayOf9(): GtvArray {
        val intArray = intArrOf9()
        val gtvList: List<Gtv> =GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))
        return gtv(gtvList)
    }

    // ----------------- 7 with 3-----------------------------
    private fun intArrOfInner3() = intArrayOf(1,9,3)

    private fun buildGtvArrInnerOf3(): GtvArray {
        val innerIntArray = intArrOfInner3()
        val innerIntArrayList =GtvTreeHelper.transformIntToGtv(innerIntArray.toCollection(ArrayList()))
        return gtv(innerIntArrayList)
    }

    fun buildGtvArrOf7WithInner3(): GtvArray {
        val intArray = intArrOf7()
        val gtvArrayList =GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))

        // Add the inner GtvArray
        val innerGtvArray = buildGtvArrInnerOf3()
        gtvArrayList.set(3, innerGtvArray)

        return gtv(gtvArrayList)
    }


}