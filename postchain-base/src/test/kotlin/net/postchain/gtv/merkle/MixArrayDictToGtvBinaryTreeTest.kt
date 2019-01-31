package net.postchain.gtv.merkle

import net.postchain.gtv.GtvPath
import net.postchain.gtv.GtvPathFactory
import org.junit.Assert
import org.junit.Test

class MixArrayDictToGtvBinaryTreeTest {

    /**
     * An arrays within a dict.
     */
    @Test
    fun testIntDictLength1withInnerLength4Array() {
        val treeHolder = MixArrayDictToGtvBinaryTreeHelper.buildTreeOfDict1WithSubArray4()
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

        val gtvPath: GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = MixArrayDictToGtvBinaryTreeHelper.buildTreeOfDict1WithSubArray4(gtvPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treeHolder.treePrintout.trim())
    }
}