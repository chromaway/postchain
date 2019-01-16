package net.postchain.base.merkle

import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.DictGTXValue
import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXValue

/**
 * Used for the case when we are mixing dicts and arrays in the same test
 */
object MixArrayDictToGtxBinaryTreeHelper {

    private val factory = GtxBinaryTreeFactory()

    fun buildTreeOfDict1WithSubArray4(): TreeHolderFromDict {
        return buildTreeOfDict1WithSubArray4(null)
    }

    fun buildTreeOfDict1WithSubArray4(gtxPath: GTXPath?): TreeHolderFromDict {
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


        // Add the inner ArrayGtxValue
        val innerIntArray = intArrayOf(1,2,3,4)
        val gtxArrayList = TreeHelper.transformIntToGTXValue(innerIntArray.toCollection(ArrayList()))
        val gtxs: Array<GTXValue> = gtxArrayList.toTypedArray()
        val innerGtxArr = ArrayGTXValue(gtxs)

        // Put the inner Array in the outer Dict
        val outerMap = HashMap<String, GTXValue>()
        outerMap.set("one", innerGtxArr)
        val gtxDict = DictGTXValue(outerMap)

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(gtxDict, listOf(gtxPath))
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