package net.postchain.gtx.merkle

import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXPathFactory
import org.junit.Assert
import org.junit.Test

class TransactionBodyToGtxBinaryTreeTest {

    /**
     *  When we only have one element, the tree should have a right dummy leaf (or else it wouldn't be "full".
     */
    @Test
    fun testBodyWith1Operation_andNoSigners() {
        val treeHolder = TransactionBodyToGtxBinaryTreeHelper.buildTreeOfBodyWith1Operation_andNoSigners()
        println(treeHolder.treePrintout)
        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testBodyWith1Operation_andNoSigners_withPath() {
        val path: Array<Any> = arrayOf(0)

        val expectedTreeWithPath = " +   \n" +
                "/ \\ \n" +
                "*MyOp - "

        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = TransactionBodyToGtxBinaryTreeHelper.buildTreeOfBodyWith1Operation_andNoSigners(gtxPath)
        println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testBodyWith2Operations_and1Signer() {
        val treeHolder = TransactionBodyToGtxBinaryTreeHelper.buildTreeOfBodyWith2Operations_andASigner()
        println(treeHolder.treePrintout)
        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testBodyWith2Operations_and1Signer_withPath() {
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
        val treeHolder = TransactionBodyToGtxBinaryTreeHelper.buildTreeOfBodyWith2Operations_andASigner(gtxPath)
        println(treeHolder.treePrintout)
        Assert.assertEquals(expected.trim(), treeHolder.treePrintout.trim())
    }
}