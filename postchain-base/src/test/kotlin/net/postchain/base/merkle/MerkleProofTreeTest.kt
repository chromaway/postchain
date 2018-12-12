package net.postchain.base.merkle

import net.postchain.gtx.GTXValue
import org.junit.Assert
import org.junit.Test

/**
 * In the comments below
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


        val value1 = treeHolder.orgGtxArray[0]
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
        val treeHolder: TreeHolder = TreeHelper.buildTreeOf7()

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

        val value4: List<GTXValue> = listOf(treeHolder.orgGtxArray[3])
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
        val treeHolder: TreeHolder = TreeHelper.buildTreeOf7()

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

        val value4and7: List<GTXValue> = listOf(treeHolder.orgGtxArray[3], treeHolder.orgGtxArray[6])
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

}