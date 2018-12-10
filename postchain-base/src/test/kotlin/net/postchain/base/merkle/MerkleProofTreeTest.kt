package net.postchain.base.merkle

import net.postchain.base.merkle.Hash
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.base.merkle.MerkleProofTreeFactory
import net.postchain.gtx.GTXValue
import net.postchain.gtx.IntegerGTXValue
import org.junit.Assert
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class MerkleProofTreeTest {

    @Test
    fun test_tree_of4() {
        val intArray = intArrayOf(1, 2, 3, 4)
        val expectedResult =
                "   +       \n" +
                "  / \\   \n" +
                " /   \\  \n" +
                " +   +   \n" +
                "/ \\ / \\ \n" +
                "1 2 3 4 \n"


        val intArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(intArrayList)

        val listOfOneGtxInt: List<GTXValue> = listOf(intArrayList[0])
        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(listOfOneGtxInt, fullBinaryTree, calculator)

        /*
        val printer = BTreePrinter()

        val treePrintout = printer.printNode(merkleProofTree)
        println(treePrintout)

        Assert.assertEquals(expectedResult.trim(), treePrintout.trim())
        */

    }

}