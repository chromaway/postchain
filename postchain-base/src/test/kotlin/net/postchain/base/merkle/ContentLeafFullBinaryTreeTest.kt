package net.postchain.base.merkle

import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.GTXValue
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentLeafFullBinaryTreeTest {


    @Test
    fun testIntArrayLength4() {
        val treeHolder = TreeHelper.buildTreeOf4()
        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength7() {
        val treeHolder = TreeHelper.buildTreeOf7()
        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntArrayLength9() {
        val treeHolder = TreeHelper.buildTreeOf9()
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

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(intArrayList)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        assertEquals(expectedTree.trim(), treePrintout.trim())
    }


    @Test
    fun testIntArrayLength7withInnerLength3Array() {
        val treeHolder = TreeHelper.buildTreeOf7WithSubTree()
        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }


}