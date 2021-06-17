// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.merkle

import net.postchain.common.data.Hash
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import net.postchain.gtv.merkleHash
import org.junit.Assert
import org.junit.Test

class MerkleRootCalculatorTest {


    val calculator = MerkleHashCalculatorDummy()

    private val empty32bytesAsHex = "0101010101010101010101010101010101010101010101010101010101010101"
    val expectedMerkleRootOf1 = "080303" + empty32bytesAsHex
    val expectedMerkleRootOf4 = "0802040404050204060407"

    @Test
    fun testStrArrayLength1_merkle_root() {
        val strArray = arrayOf("01")
        //val expectedTree = " +   \n" +
        //        "/ \\ \n" +
        //        "01 - "

        // Motivation for how the merkle root is calculated
        // ("07" is the prefix for the array head)
        // [07 + ([01 +01]) + 0000000000000000000000000000000000000000000000000000000000000000]
        // [07 + 0202       + 0000000000000000000000000000000000000000000000000000000000000000]
        // 08    0303         0101010101010101010101010101010101010101010101010101010101010101

        val listOfHashes: List<Hash> = strArray.map { TreeHelper.convertToByteArray(it) }
        val gtvArr = gtv(listOfHashes.map { gtv(it)})
        val merkleRoot = gtvArr.merkleHash(calculator)

        Assert.assertEquals(expectedMerkleRootOf1, TreeHelper.convertToHex(merkleRoot))
    }

    @Test
    fun testStrArrayLength4_merkle_root() {
        val strArray = arrayOf("01","02","03","04")
        //val expectedTree =
        //        "   +       \n" +
        //                "  / \\   \n" +
        //                " /   \\  \n" +
        //                " +   +   \n" +
        //                "/ \\ / \\ \n" +
        //                "01 02 03 04 \n"

        // Motivation for how the merkle root is calculated
        // ("07" is the prefix for the array head)
        // [07 +
        //     [00 + 0202 + 0203]
        //     +
        //     [00 + 0204 + 0205]
        //      ] ->
        // [07 +  0103030304 + 0103050306 ]
        // 08     0204040405   0204060407

        val listOfHashes: List<Hash> = strArray.map { TreeHelper.convertToByteArray(it) }
        val gtvArr = gtv(listOfHashes.map { gtv(it)})
        val merkleRoot = gtvArr.merkleHash(calculator)

        Assert.assertEquals(expectedMerkleRootOf4, TreeHelper.convertToHex(merkleRoot))
    }
}