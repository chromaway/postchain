// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.merkle

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvFactory.gtv

object ArrayToGtvBinaryTreeHelper {

    const val empty32bytesHex = "0101010101010101010101010101010101010101010101010101010101010101"
    const val expected1ElementArrayMerkleRoot = "080303" + empty32bytesHex
    const val expected4ElementArrayMerkleRoot = "0802040404050204060407"
    const val expected7ElementArrayMerkleRoot = "08020305050506030507050802030509050A040A"

    const val inner3arrayHash = "0B050707070F0608"
    const val arr7Part1Hash = "08020305050506030507"
    const val arr7Part2Hash = "02030509050A040A"
    const val expectet7and3ElementArrayMerkleRoot = arr7Part1Hash + inner3arrayHash + arr7Part2Hash

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

    fun buildGtvArrInnerOf3(): GtvArray {
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