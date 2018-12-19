package net.postchain.base.merkle

import net.postchain.gtx.GTXPath

object GtxTreeDictHelper {

    private val factory = GtxFullBinaryTreeFactory()


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


        val gtxDict = TreeHelper.transformStringAndIntToDictGTXValue(stringArray.toCollection(ArrayList()), intArray.toCollection(ArrayList()))

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(gtxDict, listOf(gtxPath))
        } else {
            factory.buildFromGtx(gtxDict)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return TreeHolderFromDict(intArray, fullBinaryTree, treePrintout, expectedTree, gtxDict)
    }
}