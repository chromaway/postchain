package net.postchain.gtv.merkle

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtv.GtvPath
import net.postchain.gtv.GtvPathFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrayToGtvBinaryTreeTest {


    /**
     *  When we only have one element, the tree should have a right dummy leaf (or else it wouldn't be "full".
     */
    @Test
    fun testIntArrayLength1() {
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf1()
        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength1_withPath() {
        val path: Array<Any> = arrayOf(0)

        val expectedTreeWithPath =
                " +   \n" +
                "/ \\ \n" +
                "*1 - "

        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf1(gtvPath)
        //println(treeHolder.treePrintout)

        assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength4() {
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf4()
        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength4_withPath() {
        val path: Array<Any> = arrayOf(3)

        val expected = "   +       \n" +
                "  / \\   \n" +
                " /   \\  \n" +
                " +   +   \n" +
                "/ \\ / \\ \n" +
                "1 2 3 *4 "

        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf4(gtvPath)
        //println(treeHolder.treePrintout)
        assertEquals(expected.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength7() {
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf7()
        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength9() {
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf9()
        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }


    @Test
    fun testIntArrayLength13() {
        val intArray = intArrayOf(1,2,3,4,5,6,7,8,9,0,1,2,3)
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

        val intArrayList =GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))

        val fullBinaryTree:GtvBinaryTree = (GtvBinaryTreeFactory()).buildFromGtv(GtvTreeHelper.transformGtvsToGtvArray(intArrayList))

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        assertEquals(expectedTree.trim(), treePrintout.trim())
    }


    /**
     * An array within an array.
     */
    @Test
    fun testIntArrayLength7withInnerLength3Array() {
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf7WithSubTree()
        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

}