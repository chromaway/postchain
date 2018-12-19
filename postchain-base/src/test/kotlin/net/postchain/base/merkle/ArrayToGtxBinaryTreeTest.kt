package net.postchain.base.merkle

import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXPathFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrayToGtxBinaryTreeTest {

    @Test
    fun testIntArrayLength1() {
        val treeHolder = GtxTreeArrayHelper.buildTreeOf1()
        println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength1_proof() {
        val path: Array<Any> = arrayOf(0)

        val expectedTreeWithPath =
                " +   \n" +
                "/ \\ \n" +
                "*1 - "

        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = GtxTreeArrayHelper.buildTreeOf1(gtxPath)
        //println(treeHolder.treePrintout)

        assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength4() {
        val treeHolder = GtxTreeArrayHelper.buildTreeOf4()
        println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength4_proof() {
        val path: Array<Any> = arrayOf(3)

        val expected = "   +       \n" +
                "  / \\   \n" +
                " /   \\  \n" +
                " +   +   \n" +
                "/ \\ / \\ \n" +
                "1 2 3 *4 "

        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = GtxTreeArrayHelper.buildTreeOf4(gtxPath)
        //println(treeHolder.treePrintout)
        assertEquals(expected.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength7() {
        val treeHolder = GtxTreeArrayHelper.buildTreeOf7()
        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength9() {
        val treeHolder = GtxTreeArrayHelper.buildTreeOf9()
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

        val intArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        val fullBinaryTree: GtxBinaryTree = (GtxFullBinaryTreeFactory()).buildFromGtx(TreeHelper.transformGTXsToArrayGTXValue(intArrayList))

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        assertEquals(expectedTree.trim(), treePrintout.trim())
    }


    @Test
    fun testIntArrayLength7withInnerLength3Array() {
        val treeHolder = GtxTreeArrayHelper.buildTreeOf7WithSubTree()
        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

}