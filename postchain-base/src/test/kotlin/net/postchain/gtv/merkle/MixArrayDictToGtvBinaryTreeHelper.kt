package net.postchain.gtv.merkle

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtv.GtvPath
import net.postchain.gtv.*
import net.postchain.gtv.GtvPathSet

/**
 * Used for the case when we are mixing dicts and arrays in the same test
 */
object MixArrayDictToGtvBinaryTreeHelper {

    private val factory =GtvBinaryTreeFactory()

    fun buildTreeOfDict1WithSubArray4(): TreeHolderFromDict {
        return buildTreeOfDict1WithSubArray4(null)
    }

    fun buildTreeOfDict1WithSubArray4(gtvPath:GtvPath?): TreeHolderFromDict {
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


        // Add the inner GtvArray
        val innerIntArray = intArrayOf(1,2,3,4)
        val gtvArrayList =GtvTreeHelper.transformIntToGtv(innerIntArray.toCollection(ArrayList()))
        val gtvs: Array<Gtv> = gtvArrayList.toTypedArray()
        val innerGtvArr = GtvArray(gtvs)

        // Put the inner Array in the outer Dict
        val outerMap = HashMap<String, Gtv>()
        outerMap.set("one", innerGtvArr)
        val gtvDict = GtvDictionary(outerMap)

        val fullBinaryTree:GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(gtvDict,GtvPathSet(setOf(gtvPath)))
        } else {
            factory.buildFromGtv(gtvDict)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return TreeHolderFromDict(innerIntArray, fullBinaryTree, treePrintout, expectedTree, gtvDict)
    }
}