package net.postchain.base.merkle.root

import net.postchain.gtx.merkle.MerkleHashCalculatorDummy
import net.postchain.base.merkle.TreeHelper
import org.junit.Assert
import org.junit.Test

class HashBinaryTreeToMerkleRootTest {

    val calculator = MerkleHashCalculatorDummy()

    val expectedMerkleRootOf1 = "A002030101010101010101010101010101010101010101010101010101010101010101"
    val expectedMerkleRootOf4 = "A001030403050103060307"


    @Test
    fun testStrArrayLength1_merkle_root() {
        val treeHolder = HashBinaryTreeHelper.buildTreeOf1()
        // Motivation for how the merkle root is calculated
        // ("A0" is the prefix for the merkle root)
        // A0 + [0102 + 0000000000000000000000000000000000000000000000000000000000000000]

        val merkleRoot = MerkleRootCalculator.computeMerkleRoot(treeHolder.clfbTree, calculator)
        val merkleRootStr = TreeHelper.convertToHex(merkleRoot)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedMerkleRootOf1, merkleRootStr)
    }

    @Test
    fun testStrArrayLength4_merkle_root() {
        val treeHolder = HashBinaryTreeHelper.buildTreeOf4()
        // Motivation for how the merkle root is calculated
        // ("A0" is the prefix for the merkle root)
        // A0 + [
        //    (00 + [0102 + 0103])
        //     +
        //     (00 + [0104 + 0105])
        //      ] ->
        // A0 + [ 0002030204 + 0002050206 ]
        // A0 0103040305 + 0103060307

        val merkleRoot = MerkleRootCalculator.computeMerkleRoot(treeHolder.clfbTree, calculator)
        val merkleRootStr = TreeHelper.convertToHex(merkleRoot)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedMerkleRootOf4, merkleRootStr)
    }

}