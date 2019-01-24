package net.postchain.gtx.merkle

import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXPathFactory
import org.junit.Assert
import org.junit.Test

class MixArrayDictToGtxBinaryTreeTest {

    /**
     * An array within a dict.
     */
    @Test
    fun testIntDictLength1withInnerLength4Array() {
        val treeHolder = MixArrayDictToGtxBinaryTreeHelper.buildTreeOfDict1WithSubArray4()
        //println(treeHolder.treePrintout)
        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntDictLength1withInnerLength4Array_withPath() {
        val path: Array<Any> = arrayOf("one", 3)

        val expectedTreeWithPath = "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   one       +       \n" +
                "          / \\   \n" +
                "         /   \\  \n" +
                " .   .   +   +   \n" +
                "        / \\ / \\ \n" +
                "- - - - 1 2 3 *4 "

        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = MixArrayDictToGtxBinaryTreeHelper.buildTreeOfDict1WithSubArray4(gtxPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }
}