package net.postchain.gtx.merkle

import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXPathFactory
import org.junit.Assert
import org.junit.Test

class OperationToGtxBinaryTreeTest {

    /**
     *  When we only have one element, the tree should have a right dummy leaf (or else it wouldn't be "full".
     */
    @Test
    fun testOperationWithNoArgs() {
        val treeHolder = OperationToGtxBinaryTreeHelper.buildTreeOfOnlyName()
        println(treeHolder.treePrintout)
        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testOperationWithNoArgs_withPath() {
        val path: Array<Any> = arrayOf(0)

        val expectedTreeWithPath = " +   \n" +
                "/ \\ \n" +
                "*ZOp - "

        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = OperationToGtxBinaryTreeHelper.buildTreeOfOnlyName(gtxPath)
        println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testOperationWith4Args() {
        val treeHolder = OperationToGtxBinaryTreeHelper.buildTreeOf4Args()
        println(treeHolder.treePrintout)
        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testOperationWith4Args_withPath() {
        val path: Array<Any> = arrayOf(4)

        val expected = "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       *4       \n" +
                "  / \\           \n" +
                " /   \\          \n" +
                " +   +   .   .   \n" +
                "/ \\ / \\         \n" +
                "MyOp 1 2 3 - - - - "

        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = OperationToGtxBinaryTreeHelper.buildTreeOf4Args(gtxPath)
        println(treeHolder.treePrintout)
        Assert.assertEquals(expected.trim(), treeHolder.treePrintout.trim())
    }
}