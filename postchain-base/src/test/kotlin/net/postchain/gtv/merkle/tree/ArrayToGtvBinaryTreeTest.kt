package net.postchain.gtv.merkle.tree

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtv.path.GtvPath
import net.postchain.gtv.path.GtvPathFactory
import net.postchain.gtv.path.GtvPathSet
import net.postchain.gtv.merkle.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrayToGtvBinaryTreeTest {

    private val factory = GtvBinaryTreeFactory()

    // ------------------- Size 1 --------------------
    /**
     *  When we only have one element, the tree should have a right dummy leaf (or else it wouldn't be "full".
     */
    private fun buildTreeOf1(): String {
        return buildTreeOf1(null)
    }

    private fun buildTreeOf1(gtvPath: GtvPath?): String {
        val gtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf1()

        val newMemoization = GtvMerkleHashMemoization(100, 100)
        val fullBinaryTree: GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(gtvArr, GtvPathSet(setOf(gtvPath)), newMemoization)
        } else {
            factory.buildFromGtv(gtvArr, newMemoization)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return treePrintout
    }


    @Test
    fun testIntArrayLength1() {
        val expectedTree = " +   \n" +
                "/ \\ \n" +
                "1 - "

        val treePrintout = buildTreeOf1()
        //println(treeHolder.treePrintout)
        assertEquals(expectedTree.trim(), treePrintout.trim())
    }

    @Test
    fun testIntArrayLength1_withPath() {
        val path: Array<Any> = arrayOf(0)

        val expectedTreeWithPath =
                " *   \n" +
                "/ \\ \n" +
                "*1 - "

        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val treePrintout = buildTreeOf1(gtvPath)
        //println(treeHolder.treePrintout)

        assertEquals(expectedTreeWithPath.trim(), treePrintout.trim())
    }

    // ------------------- Size 4 --------------------
    private fun buildTreeOf4(): String {
        return buildTreeOf4(null)
    }

    private fun buildTreeOf4(gtvPath: GtvPath?): String {

        val gtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf4()

        val newMemoization = GtvMerkleHashMemoization(100, 100)
        val fullBinaryTree:GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(gtvArr, GtvPathSet(setOf((gtvPath))), newMemoization)
        } else {
            factory.buildFromGtv(gtvArr, newMemoization)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return treePrintout
    }

    @Test
    fun testIntArrayLength4() {
        val expectedTree =
                "   +       \n" +
                        "  / \\   \n" +
                        " /   \\  \n" +
                        " +   +   \n" +
                        "/ \\ / \\ \n" +
                        "1 2 3 4 \n"

        val treePrintout = buildTreeOf4()
        //println(treeHolder.treePrintout)
        assertEquals(expectedTree.trim(), treePrintout.trim())
    }

    @Test
    fun testIntArrayLength4_withPath() {
        val path: Array<Any> = arrayOf(3)

        val expected = "   *       \n" +
                "  / \\   \n" +
                " /   \\  \n" +
                " +   +   \n" +
                "/ \\ / \\ \n" +
                "1 2 3 *4 "

        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val treePrintout = buildTreeOf4(gtvPath)
        //println(treeHolder.treePrintout)
        assertEquals(expected.trim(), treePrintout.trim())
    }

    // ------------------- Size 7 --------------------
    private fun buildTreeOf7() = buildTreeOf7(null)

    private fun buildTreeOf7(gtvPath: GtvPath?): String {
        return if (gtvPath == null) {
            buildTreeOf7(GtvPathSet(setOf()))
        } else {
            buildTreeOf7(GtvPathSet(setOf(gtvPath)))
        }
    }

    private fun buildTreeOf7(gtvPaths: GtvPathSet): String {
        val gtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf7()

        val newMemoization = GtvMerkleHashMemoization(100, 100)
        val fullBinaryTree = factory.buildFromGtvAndPath(gtvArr,gtvPaths, newMemoization)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return treePrintout
    }

    @Test
    fun testIntArrayLength7() {
        val expectedTree =
                "       +               \n" +
                        "      / \\       \n" +
                        "     /   \\      \n" +
                        "    /     \\     \n" +
                        "   /       \\    \n" +
                        "   +       +       \n" +
                        "  / \\     / \\   \n" +
                        " /   \\   /   \\  \n" +
                        " +   +   +   7   \n" +
                        "/ \\ / \\ / \\     \n" +
                        "1 2 3 4 5 6 - - "

        val treePrintout = buildTreeOf7()
        //println(treeHolder.treePrintout)
        assertEquals(expectedTree.trim(), treePrintout.trim())
    }

    @Test
    fun testIntArrayLength7_withPath() {
        val path: Array<Any> = arrayOf(6)
        val expectedTree =
                "       *               \n" +
                        "      / \\       \n" +
                        "     /   \\      \n" +
                        "    /     \\     \n" +
                        "   /       \\    \n" +
                        "   +       +       \n" +
                        "  / \\     / \\   \n" +
                        " /   \\   /   \\  \n" +
                        " +   +   +   *7   \n" +
                        "/ \\ / \\ / \\     \n" +
                        "1 2 3 4 5 6 - - "

        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val treePrintout = buildTreeOf7(gtvPath)
        //println(treePrintout)
        assertEquals(expectedTree.trim(), treePrintout.trim())
    }

    // ------------------- Size 9 --------------------

    private fun buildTreeOf9(): String {
        val gtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf9()
        val newMemoization = GtvMerkleHashMemoization(100, 100)
        val fullBinaryTree: GtvBinaryTree = factory.buildFromGtv(gtvArr, newMemoization)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return treePrintout
    }

    @Test
    fun testIntArrayLength9() {
         val expectedTree = "               +                               \n" +
                "              / \\               \n" +
                "             /   \\              \n" +
                "            /     \\             \n" +
                "           /       \\            \n" +
                "          /         \\           \n" +
                "         /           \\          \n" +
                "        /             \\         \n" +
                "       /               \\        \n" +
                "       +               9               \n" +
                "      / \\                       \n" +
                "     /   \\                      \n" +
                "    /     \\                     \n" +
                "   /       \\                    \n" +
                "   +       +       .       .       \n" +
                "  / \\     / \\                   \n" +
                " /   \\   /   \\                  \n" +
                " +   +   +   +   .   .   .   .   \n" +
                "/ \\ / \\ / \\ / \\                 \n" +
                "1 2 3 4 5 6 7 8 - - - - - - - - "

        val treePrintout = buildTreeOf9()
        //println(treeHolder.treePrintout)
        assertEquals(expectedTree.trim(), treePrintout.trim())
    }

    // ------------------- Size 13 --------------------
    private fun buildTreeOf13(): String {
        val intArray = intArrayOf(1,2,3,4,5,6,7,8,9,0,1,2,3)
        val intArrayList = GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))

        val newMemoization = GtvMerkleHashMemoization(100, 100)
        val fullBinaryTree: GtvBinaryTree = (GtvBinaryTreeFactory()).buildFromGtv(GtvTreeHelper.transformGtvsToGtvArray(intArrayList), newMemoization)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return treePrintout
    }

    @Test
    fun testIntArrayLength13() {
        val expectedTree = "               +                               \n" +
                "              / \\               \n" +
                "             /   \\              \n" +
                "            /     \\             \n" +
                "           /       \\            \n" +
                "          /         \\           \n" +
                "         /           \\          \n" +
                "        /             \\         \n" +
                "       /               \\        \n" +
                "       +               +               \n" +
                "      / \\             / \\       \n" +
                "     /   \\           /   \\      \n" +
                "    /     \\         /     \\     \n" +
                "   /       \\       /       \\    \n" +
                "   +       +       +       3       \n" +
                "  / \\     / \\     / \\           \n" +
                " /   \\   /   \\   /   \\          \n" +
                " +   +   +   +   +   +   .   .   \n" +
                "/ \\ / \\ / \\ / \\ / \\ / \\         \n" +
                "1 2 3 4 5 6 7 8 9 0 1 2 - - - - "

        val treePrintout = buildTreeOf13()
        //println(treeHolder.treePrintout)
        assertEquals(expectedTree.trim(), treePrintout.trim())
    }

    // ------------------- Size 7 with 3 --------------------
    private fun buildTreeOf7WithSubTree(): String {
        return buildTreeOf7WithSubTree(null)
    }

    private fun buildTreeOf7WithSubTree(gtvPath: GtvPath?): String {


        val gtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrOf7WithInner3()

        val newMemoization = GtvMerkleHashMemoization(100, 100)
        val fullBinaryTree: GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(gtvArr, GtvPathSet(setOf(gtvPath)), newMemoization)
        } else {
            factory.buildFromGtv(gtvArr, newMemoization)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return treePrintout
    }


    @Test
    fun testIntArrayLength7withInnerLength3Array() {
        val expectedTree = "                               +                                                               \n" +
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
                        "               +                               +                               \n" +
                        "              / \\                             / \\               \n" +
                        "             /   \\                           /   \\              \n" +
                        "            /     \\                         /     \\             \n" +
                        "           /       \\                       /       \\            \n" +
                        "          /         \\                     /         \\           \n" +
                        "         /           \\                   /           \\          \n" +
                        "        /             \\                 /             \\         \n" +
                        "       /               \\               /               \\        \n" +
                        "       +               +               +               7               \n" +
                        "      / \\             / \\             / \\                       \n" +
                        "     /   \\           /   \\           /   \\                      \n" +
                        "    /     \\         /     \\         /     \\                     \n" +
                        "   /       \\       /       \\       /       \\                    \n" +
                        "   1       2       3       +       5       6       .       .       \n" +
                        "                          / \\                                   \n" +
                        "                         /   \\                                  \n" +
                        " .   .   .   .   .   .   +   3   .   .   .   .   .   .   .   .   \n" +
                        "                        / \\                                     \n" +
                        "- - - - - - - - - - - - 1 9 - - - - - - - - - - - - - - - - - - "


        val treePrintout = buildTreeOf7WithSubTree()
        //println(treeHolder.treePrintout)
        assertEquals(expectedTree.trim(), treePrintout.trim())
    }

    @Test
    fun testIntArrayLength7withInnerLength3Array_withPath() {
        val arr: Array<Any> = arrayOf(3, 0)
        val expectedTree = "                               *                                                               \n" +
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
                "               +                               +                               \n" +
                "              / \\                             / \\               \n" +
                "             /   \\                           /   \\              \n" +
                "            /     \\                         /     \\             \n" +
                "           /       \\                       /       \\            \n" +
                "          /         \\                     /         \\           \n" +
                "         /           \\                   /           \\          \n" +
                "        /             \\                 /             \\         \n" +
                "       /               \\               /               \\        \n" +
                "       +               +               +               7               \n" +
                "      / \\             / \\             / \\                       \n" +
                "     /   \\           /   \\           /   \\                      \n" +
                "    /     \\         /     \\         /     \\                     \n" +
                "   /       \\       /       \\       /       \\                    \n" +
                "   1       2       3       *       5       6       .       .       \n" +
                "                          / \\                                   \n" +
                "                         /   \\                                  \n" +
                " .   .   .   .   .   .   +   3   .   .   .   .   .   .   .   .   \n" +
                "                        / \\                                     \n" +
                "- - - - - - - - - - - - *1 9 - - - - - - - - - - - - - - - - - - "


        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(arr)
        val treePrintout = buildTreeOf7WithSubTree(gtvPath)
        //println(treeHolder.treePrintout)
        assertEquals(expectedTree.trim(), treePrintout.trim())
    }
}