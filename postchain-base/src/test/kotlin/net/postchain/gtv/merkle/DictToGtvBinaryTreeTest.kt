package net.postchain.gtv.merkle

import net.postchain.gtv.GtvPath
import net.postchain.gtv.GtvPathFactory
import org.junit.Assert
import org.junit.Test

class DictToGtvBinaryTreeTest {

    @Test
    fun testIntDictLength1() {
        val treeHolderFromDict = DictToGtvBinaryTreeHelper.buildThreeOf1_fromDict()
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

        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtvBinaryTreeHelper.buildThreeOf1_fromDict(gtvPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testIntDictLength4() {
        val treeHolderFromDict = DictToGtvBinaryTreeHelper.buildThreeOf4_fromDict()
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

        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtvBinaryTreeHelper.buildThreeOf4_fromDict(gtvPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }

    /**
     * A dict within a dict.
     */
    @Test
    fun testIntDictLength1withInnerLength2Dict() {
        val treeHolder = DictToGtvBinaryTreeHelper.buildTreeOf1WithSubTree()
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

        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtvBinaryTreeHelper.buildTreeOf1WithSubTree(gtvPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }

}