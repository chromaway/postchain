package net.postchain.base.merkle

import net.postchain.gtx.GTXValue
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
 * In the comments below
 *   "<...>" means "serialization" and
 *   "[ .. ]" means "hash" and
 *   "(a + b)" means append "b" after "a" into "ab"
 *
 * Since we are using the dummy hash function, all binary numbers will be +1 after hash function
 *  02 -> 03 etc.
 *
 * The dummy serializer doesn't do anything but converting an int to a byte:
 *   7 -> 07
 *   12 -> 0C
 */
class MerkleProofTreeTest {

    val expected1ElementArrayMerkleRoot = "0702030101010101010101010101010101010101010101010101010101010101010101"
    val expected4ElementArrayMerkleRoot = "0701030403050103060307"

    @Test
    fun test_tree_of1() {
        val treeHolder = GtxTreeHelper.buildTreeOf1()

        val expectedPath =
                " +   \n" +
                "/ \\ \n" +
                "1 0000000000000000000000000000000000000000000000000000000000000000 "

        val value1 = treeHolder.orgGtxList[0]
        val listOfOneGtxInt: List<GTXValue> = listOf(value1)
        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(listOfOneGtxInt, treeHolder.clfbTree, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedPath.trim(), resultPrintout.trim())

    }

    @Test
    fun test_tree_of1_merkle_root() {
        val treeHolder = GtxTreeHelper.buildTreeOf1()

        val value1 = treeHolder.orgGtxList[0]
        val listOfOneGtxInt: List<GTXValue> = listOf(value1)
        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(listOfOneGtxInt, treeHolder.clfbTree, calculator)

        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expected1ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))
    }

    @Test
    fun test_tree_of4() {
        val treeHolder = GtxTreeHelper.buildTreeOf4()

        // This is how the (dummy = +1) hash calculation works done for the right side of the path:
        //
        // 00 + [(01 + [03]) + (01 + [04])]
        // 00 + [(01 + 04) + (01 + 05)]
        // 00 + [0104 + 0105] <-- Now we have the hash of the leaf "3" (=0104) and leaf "4" (0105).
        // 00 + [01040105]
        // 00 + 02050206
        // 0002050206  <-- Combined hash of 3 and 4.

        val expectedPath =
                "   +       \n" +
                "  / \\   \n" +
                " /   \\  \n" +
                " +   0002050206   \n" +
                "/ \\     \n" +
                "1 0103 - - "


        val value1 = treeHolder.orgGtxList[0]
        val listOfOneGtxInt: List<GTXValue> = listOf(value1)
        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(listOfOneGtxInt, treeHolder.clfbTree, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedPath.trim(), resultPrintout.trim())

    }

    @Test
    fun test_tree_of4_merkle_root() {
        val treeHolder = GtxTreeHelper.buildTreeOf4()

        // How to calculate the root of the proof above:
        // (see the test above for where we get these numbers)
        // 07 + [
        //       00 [(01 + [<1>]) + 0103] +
        //       0002050206
        //      ] ->
        // 07 + [
        //       00 [0102 + 0103] +
        //       0002050206
        //      ] ->
        // 07 + [ 0002030204 + 0002050206 ] ->
        // 07     0103040305 + 0103060307 ->
        // 0701030403050103060307

        val value1 = treeHolder.orgGtxList[0]
        val listOfOneGtxInt: List<GTXValue> = listOf(value1)
        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(listOfOneGtxInt, treeHolder.clfbTree, calculator)

        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expected4ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))
    }



    @Test
    fun test_tree_of7() {
        val treeHolder: TreeHolderFromArray = GtxTreeHelper.buildTreeOf7()

        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())

        // This is how the (dummy = +1) hash calculation works done for the right side of the path:
        //
        // 00 + [
        //        (00 + [(01 + [05]) +
        //               (01 + [06])])
        //        +
        //        (01 + [07])
        //      ]
        // 00 + [(00 + [0106 + 0107]) + 0108 ]
        // 00 + [00 + 02070208 + 0108 ]
        // 00 + [00020702080108 ]
        // 00 +  01030803090209
        val expectedPath = "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       0001030803090209       \n" +
                "  / \\           \n" +
                " /   \\          \n" +
                " 0002030204   +   .   .   \n" +
                "    / \\         \n" +
                "- - 0104 4 - - - - "

        val value4: List<GTXValue> = listOf(treeHolder.orgGtxList[3])
        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(value4, treeHolder.clfbTree, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedPath.trim(), resultPrintout.trim())

    }


    @Test
    fun test_tree_of7_with_double_proof() {
        val treeHolder: TreeHolderFromArray = GtxTreeHelper.buildTreeOf7()

        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())

        val expectedPath =
                "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       +       \n" +
                "  / \\     / \\   \n" +
                " /   \\   /   \\  \n" +
                " 0002030204   +   0002070208   7   \n" +
                "    / \\         \n" +
                "- - 0104 4 - - - - "

        val value4and7: List<GTXValue> = listOf(treeHolder.orgGtxList[3], treeHolder.orgGtxList[6])
        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(value4and7, treeHolder.clfbTree, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedPath.trim(), resultPrintout.trim())

    }

    @Test
    fun test_ArrayLength7_withInnerLength3Array() {
        val treeHolder = GtxTreeHelper.buildTreeOf7WithSubTree()

        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())

        val expectedPath =
                "                               +                                                               \n" +
                "                              / \\                               \n" +
                "                             /   \\                              \n" +
                "                            /     \\                             \n" +
                "                           /       \\                            \n" +
                "                          /         \\                           \n" +
                "                         /           \\                          \n" +
                "                        /             \\                         \n" +
                "                       /               \\                        \n" +
                "                      /                 \\                       \n" +
                "                     /                   \\                      \n" +
                "                    /                     \\                     \n" +
                "                   /                       \\                    \n" +
                "                  /                         \\                   \n" +
                "                 /                           \\                  \n" +
                "                /                             \\                 \n" +
                "               /                               \\                \n" +
                "               +                               0001030803090209                               \n" +
                "              / \\                                               \n" +
                "             /   \\                                              \n" +
                "            /     \\                                             \n" +
                "           /       \\                                            \n" +
                "          /         \\                                           \n" +
                "         /           \\                                          \n" +
                "        /             \\                                         \n" +
                "       /               \\                                        \n" +
                "       0002030204               +               .               .               \n" +
                "                      / \\                                       \n" +
                "                     /   \\                                      \n" +
                "                    /     \\                                     \n" +
                "                   /       \\                                    \n" +
                "   .       .       0104       +       .       .       .       .       \n" +
                "                          / \\                                   \n" +
                "                         /   \\                                  \n" +
                " .   .   .   .   .   .   +   0104   .   .   .   .   .   .   .   .   \n" +
                "                        / \\                                     \n" +
                "- - - - - - - - - - - - 0102 9 - - - - - - - - - - - - - - - - - - "


        val theNineLeaf = treeHolder.orgGtxSubArray[1]  // This is the one with a "9" in it.
        val listOfOneGtxInt: List<GTXValue> = listOf(theNineLeaf)
        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(listOfOneGtxInt, treeHolder.clfbTree, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedPath.trim(), resultPrintout.trim())
    }

    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2three() {
        val treeHolder = GtxTreeHelper.buildTreeOf7WithSubTree()

        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())

        // How to calculate the hash of the sub tree?
        // 07 + [
        //        (00 + [(01 + [01]) + (01 + [09])] )
        //         +
        //        (01 + [03])
        //      ] ->
        // 07 + [
        //        (00 + [0102 + 010A] )
        //         +
        //        (01 + [03])
        //      ] ->
        // 07 + [
        //        (00 + 0203020B)
        //         +
        //        (01 + 04)
        //      ] ->
        // 07 + [ 000203020B0104 ] ->
        // 07 +   010304030C0205
        val expectedPath =
                "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       0001030803090209       \n" +
                "  / \\           \n" +
                " /   \\          \n" +
                " 0002030204   +   .   .   \n" +
                "    / \\         \n" +
                "- - 3 07010304030C0205 - - - - "


        val theThreeLeaf = treeHolder.orgGtxList[2]  // This is the one with a "3" in it.
        val listOfOneGtxInt: List<GTXValue> = listOf(theThreeLeaf)
        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(listOfOneGtxInt, treeHolder.clfbTree, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        println(resultPrintout)

        Assert.assertEquals(expectedPath.trim(), resultPrintout.trim())
    }



}