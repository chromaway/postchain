package net.postchain.base.merkle

import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import org.junit.Assert
import org.junit.Test

class MerkleRootCalculatorTest {


    val calculator = MerkleHashCalculatorDummy()
    val merkleRootCalculator = MerkleRootCalculator(calculator)

    val expectedMerkleRootOf1 = "0702030101010101010101010101010101010101010101010101010101010101010101"
    val expectedMerkleRootOf4 = "0701030403050103060307"


    @Test
    fun testStrArrayLength1_merkle_root() {
        val strArray = arrayOf("01")
        //val expectedTree = " +   \n" +
        //        "/ \\ \n" +
        //        "0102 - "

        // Motivation for how the merkle root is calculated
        // ("07" is the prefix for the array head)
        // 07 + [(01 +[01]) + 0000000000000000000000000000000000000000000000000000000000000000]
        // 07 + [0102 + 0000000000000000000000000000000000000000000000000000000000000000]
        // 07    0203   0101010101010101010101010101010101010101010101010101010101010101

        val listOfHashes: List<Hash> = strArray.map { TreeHelper.convertToByteArray(it) }
        val merkleRoot = merkleRootCalculator.calculateMerkleRoot(listOfHashes)

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
        // 07 + [
        //     (00 + [0102 + 0103])
        //     +
        //     (00 + [0104 + 0105])
        //      ] ->
        // 07 + [ 0002030204 + 0002050206 ]
        // 07     0103040305   0103060307

        val listOfHashes: List<Hash> = strArray.map { TreeHelper.convertToByteArray(it) }
        val merkleRoot = merkleRootCalculator.calculateMerkleRoot(listOfHashes)

        Assert.assertEquals(expectedMerkleRootOf4, TreeHelper.convertToHex(merkleRoot))
    }
}