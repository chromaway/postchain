package net.postchain.base.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.gtv.path.GtvPath
import net.postchain.gtv.path.GtvPathFactory
import net.postchain.gtv.path.GtvPathSet
import net.postchain.gtv.generateProof
import net.postchain.gtv.merkle.*
import net.postchain.gtv.merkle.proof.merkleHash
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
 * We will generate proofs from mixes of Dict and Arrays
 */
class MixArrayDictToMerkleProofTreeTest {




    @Test
    fun test_dictWithArr_where_path_is_to_leaf4() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf("one", 3)
        val gtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val gtvDict = MixArrayDictToGtvBinaryTreeHelper.buildGtvDictWithSubArray4()

        val expectedTree = "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   02706F66       *       \n" +
                "          / \\   \n" +
                "         /   \\  \n" +
                " .   .   0103030304   +   \n" +
                "            / \\ \n" +
                "- - - - - - 0204 *4 "

        val merkleProofTree = gtvDict.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHash(calculator)
        assertEquals(MixArrayDictToGtvBinaryTreeHelper.expecedMerkleRoot_dict1_array4, TreeHelper.convertToHex(merkleProofRoot))
    }

    /**
     * Here we try to prove the entire sub array (1,2,3,4)
     */
    @Test
    fun test_dictWithArr_where_path_is_to_sub_arr() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf("one")
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val gtvDict = MixArrayDictToGtvBinaryTreeHelper.buildGtvDictWithSubArray4()

        val expectedTree = " +   \n" +
                "/ \\ \n" +
                "02706F66 *GtvArray(array=[GtvInteger(integer=1), GtvInteger(integer=2), GtvInteger(integer=3), GtvInteger(integer=4)])"

        val merkleProofTree = gtvDict.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHash(calculator)
        assertEquals(MixArrayDictToGtvBinaryTreeHelper.expecedMerkleRoot_dict1_array4, TreeHelper.convertToHex(merkleProofRoot))
    }
}