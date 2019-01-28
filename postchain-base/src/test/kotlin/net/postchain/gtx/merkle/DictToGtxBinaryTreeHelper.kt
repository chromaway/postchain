package net.postchain.gtx.merkle

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreeHelper
import net.postchain.gtx.merkle.TreeHolderFromDict
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtx.DictGTXValue
import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXPathSet
import net.postchain.gtx.GTXValue

object DictToGtxBinaryTreeHelper {

    private val factory = GtxBinaryTreeFactory()

    /**
     * When we only have one element in the Dict we don't have to generate dummies, since a dict will always have even pairs.
     */
    fun buildThreeOf1_fromDict(): TreeHolderFromDict {
        return buildThreeOf1_fromDict(null)
    }

    fun buildThreeOf1_fromDict(gtxPath: GTXPath?): TreeHolderFromDict {
        val stringArray = arrayOf("one")
        val intArray = intArrayOf(1)
        val expectedTree =
                " +   \n" +
                "/ \\ \n" +
                "one 1"


        val gtxDict = GtxTreeHelper.transformStringAndIntToDictGTXValue(stringArray.toCollection(ArrayList()), intArray.toCollection(ArrayList()))

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(gtxDict, GTXPathSet(setOf(gtxPath)))
        } else {
            factory.buildFromGtx(gtxDict)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return TreeHolderFromDict(intArray, fullBinaryTree, treePrintout, expectedTree, gtxDict)
    }

    fun buildThreeOf4_fromDict(): TreeHolderFromDict {
        return buildThreeOf4_fromDict(null)
    }


    fun buildThreeOf4_fromDict(gtxPath: GTXPath?): TreeHolderFromDict {
        val stringArray = arrayOf("one","two","three","four")
        val intArray = intArrayOf(1,2,3,4)
        val expectedTree =
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
                        "four 4 one 1 three 3 two 2 "


        val gtxDict = GtxTreeHelper.transformStringAndIntToDictGTXValue(stringArray.toCollection(ArrayList()), intArray.toCollection(ArrayList()))

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(gtxDict, GTXPathSet(setOf(gtxPath)))
        } else {
            factory.buildFromGtx(gtxDict)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return TreeHolderFromDict(intArray, fullBinaryTree, treePrintout, expectedTree, gtxDict)
    }

    /**
     * Dict within a dict
     */
    fun buildTreeOf1WithSubTree(): TreeHolderFromDict {
        return buildTreeOf1WithSubTree(null)
    }

    fun buildTreeOf1WithSubTree(gtxPath: GTXPath?): TreeHolderFromDict {
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
                "- - - - eight 8 seven 7 "


        // Add the inner DictGtxValue
        val innerStringArray = arrayOf("seven", "eight")
        val innerIntArray = intArrayOf(7, 8)
        val innerGtxDict = GtxTreeHelper.transformStringAndIntToDictGTXValue(innerStringArray.toCollection(ArrayList()), innerIntArray.toCollection(ArrayList()))

        // Put the inner Dict in the outer Dict
        val outerMap = HashMap<String, GTXValue>()
        outerMap.set("one", innerGtxDict)
        val gtxDict = DictGTXValue(outerMap)

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(gtxDict, GTXPathSet(setOf(gtxPath)))
        } else {
            factory.buildFromGtx(gtxDict)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return TreeHolderFromDict(innerIntArray, fullBinaryTree, treePrintout, expectedTree, gtxDict)
    }
}