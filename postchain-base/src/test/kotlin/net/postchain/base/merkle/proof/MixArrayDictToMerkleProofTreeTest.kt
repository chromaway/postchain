package net.postchain.base.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXPathFactory
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
 * We will generate proofs from mixes of Dict and Arrays
 */
class MixArrayDictToMerkleProofTreeTest {


    val calculator = MerkleHashCalculatorDummy()
    val factory = GtxMerkleProofTreeFactory(calculator)

    val expecedMerkleRoot_dict1_array4 = "08027170670802040504060204070408"

    @Test
    fun test_tree_from_dict_with_arr_where_path_is_to_leaf4() {
        val path: Array<Any> = arrayOf("one", 3)
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = MixArrayDictToGtxBinaryTreeHelper.buildTreeOfDict1WithSubArray4(gtxPath)

        val expectedPath = "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   01706F66       +       \n" +
                "          / \\   \n" +
                "         /   \\  \n" +
                " .   .   0002030204   +   \n" +
                "            / \\ \n" +
                "- - - - - - 0104 *4 "

        val merkleProofTree: GtxMerkleProofTree = factory.buildGtxMerkleProofTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedPath.trim(), resultPrintout.trim())
    }

    @Test
    fun test_tree_from_dict_with_arr_where_path_is_to_leaf4_proof() {
        val path: Array<Any> = arrayOf("one", 3)
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = MixArrayDictToGtxBinaryTreeHelper.buildTreeOfDict1WithSubArray4(gtxPath)

        // 08 + [ (01 + [<one>])
        //      +
        //      (0701030403050103060307) <-- Stole this from test "test_tree_of4_merkle_root()"
        //      ] ->
        // 08 + [ 01 + [6F6E65]
        //      +
        //      (0701030403050103060307)
        //      ] ->
        // 08 + [ 01 + 706F66 + 0701030403050103060307] ->
        // 08 + 02 + 717067 + 0802040504060204070408 ->
        // 08027170670802040504060204070408

        val merkleProofTree: GtxMerkleProofTree = factory.buildGtxMerkleProofTree(treeHolder.clfbTree)

        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expecedMerkleRoot_dict1_array4, TreeHelper.convertToHex(merkleProofRoot))
    }

    /**
     * Here we try to prove the entire sub array (1,2,3,4)
     */
    @Test
    fun test_tree_from_dict_with_arr_where_path_is_to_sub_arr() {
        val path: Array<Any> = arrayOf("one")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = MixArrayDictToGtxBinaryTreeHelper.buildTreeOfDict1WithSubArray4(gtxPath)

        val expectedPath = " +   \n" +
                "/ \\ \n" +
                "01706F66 *ArrayGTXValue(array=[IntegerGTXValue(integer=1), IntegerGTXValue(integer=2), IntegerGTXValue(integer=3), IntegerGTXValue(integer=4)])"

        val merkleProofTree: GtxMerkleProofTree = factory.buildGtxMerkleProofTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        println(resultPrintout)

        Assert.assertEquals(expectedPath.trim(), resultPrintout.trim())

    }

    @Test
    fun test_tree_from_dict_with_arr_where_path_is_sub_arr_proof() {
        val path: Array<Any> = arrayOf("one")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = MixArrayDictToGtxBinaryTreeHelper.buildTreeOfDict1WithSubArray4(gtxPath)

        val merkleProofTree: GtxMerkleProofTree = factory.buildGtxMerkleProofTree(treeHolder.clfbTree)

        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expecedMerkleRoot_dict1_array4, TreeHelper.convertToHex(merkleProofRoot))
    }

}