package net.postchain.gtv.merkle.tree

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtv.GtvPath
import net.postchain.gtv.GtvPathFactory
import net.postchain.gtv.GtvPathSet
import net.postchain.gtv.merkle.*
import org.junit.Assert
import org.junit.Test

class MixArrayDictToGtvBinaryTreeTest {

    private val factory = GtvBinaryTreeFactory()

    /**
     * An arrays within a dict.
     */
    private fun buildTreeOfDict1WithSubArray4(): String {
        return buildTreeOfDict1WithSubArray4(null)
    }

    private fun buildTreeOfDict1WithSubArray4(gtvPath:GtvPath?): String {
        val gtvDict = MixArrayDictToGtvBinaryTreeHelper.buildGtvDictWithSubArray4()

        val newMemoization = GtvMerkleHashMemoization(100, 100)
        val fullBinaryTree: GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(gtvDict, GtvPathSet(setOf(gtvPath)), newMemoization)
        } else {
            factory.buildFromGtv(gtvDict, newMemoization)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return treePrintout
    }

    @Test
    fun testIntDictLength1withInnerLength4Array() {
        val expectedTree =
                "       +               \n" +
                        "      / \\       \n" +
                        "     /   \\      \n" +
                        "    /     \\     \n" +
                        "   /       \\    \n" +
                        "   one       +       \n" +
                        "          / \\   \n" +
                        "         /   \\  \n" +
                        " .   .   +   +   \n" +
                        "        / \\ / \\ \n" +
                        "- - - - 1 2 3 4 "


        val treePrintout = buildTreeOfDict1WithSubArray4()
        //println(treeHolder.treePrintout)
        Assert.assertEquals(expectedTree.trim(), treePrintout.trim())
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
        val treePrintout = buildTreeOfDict1WithSubArray4(gtvPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treePrintout.trim())
    }
}