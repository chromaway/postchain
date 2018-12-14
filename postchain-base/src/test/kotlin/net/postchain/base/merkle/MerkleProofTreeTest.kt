package net.postchain.base.merkle

import net.postchain.gtx.GTXValue
import org.junit.Assert
import org.junit.Test

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

    @Test
    fun test_tree_of4() {
        val treeHolder = TreeHelper.buildTreeOf4()

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
    fun test_tree_of7() {
        val treeHolder: TreeHolderFromArray = TreeHelper.buildTreeOf7()

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
        val treeHolder: TreeHolderFromArray = TreeHelper.buildTreeOf7()

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
        val treeHolder = TreeHelper.buildTreeOf7WithSubTree()

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
    fun test_tree_from_4dict() {
        val treeHolder = TreeHelper.buildThreeOf4_fromDict()

        // This is how the (dummy = +1) hash calculation works done for the right side of the path:
        //
        //
        // "four" serialized becomes bytes: 666F7572
        // "one" serialized becomes bytes: 6F6E65
        // "three" serialized becomes bytes: 7468726565
        // "two" serialized becomes bytes: 74776F
        //
        // 00 + [(00 +
        //         [
        //            (01 + [<three>]) +
        //            (01 + [<3>])
        //         ])
        //       (00 +
        //         [
        //            (01 + [<two>]) +
        //            (01 + [<2>])
        // 00 + [(00 +
        //         [
        //            (01 + [7468726565]) +
        //            (01 + [03])
        //         ])
        //       (00 +
        //         [
        //            (01 + [74776F]) +
        //            (01 + [02])
        //         ])
        //         ])
        // 00 + [(00 + [017569736666 + 0104)] +
        //      [(00 + [01757870 + 0103)])
        // 00 + [(00 + 02766A746767 0205)] +
        //      [(00 + 02767971 0204])
        // 00 + 01 03776B756868 0306 +
        //      01 03777A72 0305
        // 000103776B75686803060103777A720305
        //
        val expectedPath =
                "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       000103776B75686803060103777A720305       \n" +
                "  / \\           \n" +
                " /   \\          \n" +
                " +   00027170670203   .   .   \n" +
                "/ \\             \n" +
                "0167707673 4 - - - - - - "

        val key4 = "four"
        val value4 = treeHolder.orgGtxDict.get(key4)
        val listOfOneGtxInt: List<GTXValue> = listOf(value4!!)
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
    fun test_tree_from_4dict_prove_the_pair() {
        val treeHolder = TreeHelper.buildThreeOf4_fromDict()

        val expectedPath =
                "       +               \n" +
                        "      / \\       \n" +
                        "     /   \\      \n" +
                        "    /     \\     \n" +
                        "   /       \\    \n" +
                        "   +       000103776B75686803060103777A720305       \n" +
                        "  / \\           \n" +
                        " /   \\          \n" +
                        " +   00027170670203   .   .   \n" +
                        "/ \\             \n" +
                        "four 4 - - - - - - "

        val treeStrFinder = TreeElementFinder<String>()
        val strGtxValue4List = treeStrFinder.findGtxValueFromPrimitiveType("four", treeHolder.clfbTree.root)
        val strGtxValue4 = strGtxValue4List.get(0)

        val treeIntFinder = TreeElementFinder<Int>()
        val intGtxValue4List = treeIntFinder.findGtxValueFromPrimitiveType(4, treeHolder.clfbTree.root)
        val intGtxValue4 = intGtxValue4List.get(0)

        val listOfTwoGtxValues: List<GTXValue> = listOf(intGtxValue4, strGtxValue4)
        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(listOfTwoGtxValues, treeHolder.clfbTree, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedPath.trim(), resultPrintout.trim())

    }

}