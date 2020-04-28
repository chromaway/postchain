// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.merkle.tree

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtv.merkle.DictToGtvBinaryTreeHelper
import net.postchain.gtv.merkle.GtvBinaryTree
import net.postchain.gtv.merkle.GtvBinaryTreeFactory
import net.postchain.gtv.path.GtvPath
import net.postchain.gtv.path.GtvPathFactory
import net.postchain.gtv.path.GtvPathSet
import org.junit.Assert
import org.junit.Test

class DictToGtvBinaryTreeTest {

    private val ln = System.lineSeparator()
    private val factory = GtvBinaryTreeFactory()

    // ------------------- Size 1 --------------------
    private fun buildThreeOf1_fromDict(): String {
        return buildThreeOf1_fromDict(null)
    }

    private fun buildThreeOf1_fromDict(gtvPath: GtvPath?): String {

        val gtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1()

        val fullBinaryTree: GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(gtvDict, GtvPathSet(setOf(gtvPath)))
        } else {
            factory.buildFromGtv(gtvDict)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return treePrintout
    }

    @Test
    fun testIntDictLength1() {
        val expectedTree = " +   $ln" +
                "/ \\ $ln" +
                "one 1"

        val treePrintout = buildThreeOf1_fromDict()
        //println(treeHolderFromDict.treePrintout)
        Assert.assertEquals(expectedTree.trim(), treePrintout.trim())
    }

    @Test
    fun testIntDictLength1_withPath() {
        val path: Array<Any> = arrayOf("one")

        val expectedTreeWithPath = " *   $ln" +
                "/ \\ $ln" +
                "one *1 "

        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val treePrintout = buildThreeOf1_fromDict(gtvPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treePrintout.trim())
    }

    // ------------------- Size 4 --------------------
    private fun buildThreeOf4_fromDict(): String {
        return buildThreeOf4_fromDict(null)
    }

    private fun buildThreeOf4_fromDict(gtvPath: GtvPath?): String {
        val gtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf4()

        val fullBinaryTree: GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(gtvDict, GtvPathSet(setOf(gtvPath)))
        } else {
            factory.buildFromGtv(gtvDict)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return treePrintout
    }

    @Test
    fun testIntDictLength4() {

        val expectedTree = "       +               $ln" +
                "      / \\       $ln" +
                "     /   \\      $ln" +
                "    /     \\     $ln" +
                "   /       \\    $ln" +
                "   +       +       $ln" +
                "  / \\     / \\   $ln" +
                " /   \\   /   \\  $ln" +
                " +   +   +   +   $ln" +
                "/ \\ / \\ / \\ / \\ $ln" +
                "four 4 one 1 three 3 two 2 "

        val treePrintout = buildThreeOf4_fromDict()
        //println(treeHolderFromDict.treePrintout)
        Assert.assertEquals(expectedTree.trim(), treePrintout.trim())
    }

    @Test
    fun testIntDictLength4_withPath() {
        val path: Array<Any> = arrayOf("one")

        val expectedTreeWithPath = "       *               $ln" +
                "      / \\       $ln" +
                "     /   \\      $ln" +
                "    /     \\     $ln" +
                "   /       \\    $ln" +
                "   +       +       $ln" +
                "  / \\     / \\   $ln" +
                " /   \\   /   \\  $ln" +
                " +   +   +   +   $ln" +
                "/ \\ / \\ / \\ / \\ $ln" +
                "four 4 one *1 three 3 two 2 "

        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val treePrintout = buildThreeOf4_fromDict(gtvPath)
        println(treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treePrintout.trim())
    }

    // ------------------- Size 1 in 2 --------------------
    /**
     * A dict within a dict.
     */

    fun buildTreeOf1WithSubTree(): String {
        return buildTreeOf1WithSubTree(null)
    }

    fun buildTreeOf1WithSubTree(gtvPath: GtvPath?): String {
        val gtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1WithSubDictOf2()

        val fullBinaryTree: GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(gtvDict, GtvPathSet(setOf(gtvPath)))
        } else {
            factory.buildFromGtv(gtvDict)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return treePrintout
    }

    @Test
    fun testIntDictLength1withInnerLength2Dict() {
        val expectedTree = "       +               $ln" +
                "      / \\       $ln" +
                "     /   \\      $ln" +
                "    /     \\     $ln" +
                "   /       \\    $ln" +
                "   one       +       $ln" +
                "          / \\   $ln" +
                "         /   \\  $ln" +
                " .   .   +   +   $ln" +
                "        / \\ / \\ $ln" +
                "- - - - eight 8 seven 7 "

        val treePrintout = buildTreeOf1WithSubTree()
        //println(treeHolder.treePrintout)
        Assert.assertEquals(expectedTree.trim(), treePrintout.trim())
    }

    @Test
    fun testIntDictLength1withInnerLength2Dict_withPath() {
        val path: Array<Any> = arrayOf("one", "seven")

        val expectedTreeWithPath = "       *               $ln" +
                "      / \\       $ln" +
                "     /   \\      $ln" +
                "    /     \\     $ln" +
                "   /       \\    $ln" +
                "   one       *       $ln" +
                "          / \\   $ln" +
                "         /   \\  $ln" +
                " .   .   +   +   $ln" +
                "        / \\ / \\ $ln" +
                "- - - - eight 8 seven *7 "

        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val treePrintout = buildTreeOf1WithSubTree(gtvPath)
        //println(treeHolder.treePrintout)

        Assert.assertEquals(expectedTreeWithPath.trim(), treePrintout.trim())
    }

}