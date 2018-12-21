package net.postchain.base.merkle

import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXPathFactory
import org.junit.Assert
import org.junit.Test

class DictToGtxBinaryTreeTest {

    @Test
    fun testIntDictLength1() {
        val treeHolderFromDict = DictToGtxBinaryTreeHelper.buildThreeOf1_fromDict()
        //println(treeHolderFromDict.treePrintout)
        Assert.assertEquals(treeHolderFromDict.expectedPrintout.trim(), treeHolderFromDict.treePrintout.trim())
    }

    @Test
    fun testIntDictLength1_withPath() {
        val path: Array<Any> = arrayOf("one")

        val expectedTreeWithPath =
                " +   \n" +
                "/ \\ \n" +
                "one *1 "

        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildThreeOf1_fromDict(gtxPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntDictLength4() {
        val treeHolderFromDict = DictToGtxBinaryTreeHelper.buildThreeOf4_fromDict()
        //println(treeHolderFromDict.treePrintout)
        Assert.assertEquals(treeHolderFromDict.expectedPrintout.trim(), treeHolderFromDict.treePrintout.trim())
    }

    @Test
    fun testIntDictLength4_withPath() {
        val path: Array<Any> = arrayOf("one")

        val expectedTreeWithPath =
                "       +               \n" +
                        "      / \\       \n" +
                        "     /   \\      \n" +
                        "    /     \\     \n" +
                        "   /       \\    \n" +
                        "   +       +       \n" +
                        "  / \\     / \\   \n" +
                        " /   \\   /   \\  \n" +
                        " +   +   +   +   \n" +
                        "/ \\ / \\ / \\ / \\ \n" +
                        "four 4 one *1 three 3 two 2 "

        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildThreeOf4_fromDict(gtxPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }

    /**
     * A dict within a dict.
     */
    @Test
    fun testIntDictLength1withInnerLength2Dict() {
        val treeHolder = DictToGtxBinaryTreeHelper.buildTreeOf1WithSubTree()
        //println(treeHolder.treePrintout)
        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntDictLength1withInnerLength2Dict_withPath() {
        val path: Array<Any> = arrayOf("one", "seven")

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
                "- - - - eight 8 seven *7 "

        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildTreeOf1WithSubTree(gtxPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }

}