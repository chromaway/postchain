package net.postchain.base.merkle

import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXValue

object ArrayToGtxBinaryTreeHelper {


    private val factory = GtxFullBinaryTreeFactory()


    /**
     * Use this if you don't have a path to prove
     */
    fun buildTreeOf1(): TreeHolderFromArray {
        return buildTreeOf1(null)
    }

    fun buildTreeOf1(gtxPath: GTXPath?): TreeHolderFromArray {
        val intArray = intArrayOf(1)
        val expectedTree =
                " +   \n" +
                "/ \\ \n" +
                "1 - "

        val gtxArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))
        val gtxArr: Array<GTXValue> = gtxArrayList.toTypedArray()

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(ArrayGTXValue(gtxArr), listOf(gtxPath))
        } else {
            factory.buildFromGtx(ArrayGTXValue(gtxArr))
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, gtxArrayList)
    }


    fun buildTreeOf4(): TreeHolderFromArray {
        return buildTreeOf4(null)
    }

    fun buildTreeOf4(gtxPath: GTXPath?): TreeHolderFromArray {
        val intArray = intArrayOf(1,2,3,4)
        val expectedTree =
                "   +       \n" +
                        "  / \\   \n" +
                        " /   \\  \n" +
                        " +   +   \n" +
                        "/ \\ / \\ \n" +
                        "1 2 3 4 \n"

        val gtxArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))
        val gtxArr: Array<GTXValue> = gtxArrayList.toTypedArray()
        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(ArrayGTXValue(gtxArr), listOf(gtxPath))
        } else {
            factory.buildFromGtx(ArrayGTXValue(gtxArr))
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, gtxArrayList)
    }

    fun buildTreeOf7(): TreeHolderFromArray {
        return buildTreeOf7(null)
    }

    fun buildTreeOf7(gtxPath: GTXPath?): TreeHolderFromArray {
        return if (gtxPath == null) {
            buildTreeOf7(listOf())
        } else {
            buildTreeOf7(listOf(gtxPath))
        }
    }

    fun buildTreeOf7(gtxPaths: List<GTXPath>): TreeHolderFromArray {
        val intArray = intArrayOf(1, 2, 3, 4, 5, 6, 7)

        val expectedTree =
                "       +               \n" +
                        "      / \\       \n" +
                        "     /   \\      \n" +
                        "    /     \\     \n" +
                        "   /       \\    \n" +
                        "   +       +       \n" +
                        "  / \\     / \\   \n" +
                        " /   \\   /   \\  \n" +
                        " +   +   +   7   \n" +
                        "/ \\ / \\ / \\     \n" +
                        "1 2 3 4 5 6 - - "



        val gtxList: List<GTXValue> = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))
        val gtxArr: Array<GTXValue> = gtxList.toTypedArray()

        val fullBinaryTree = factory.buildFromGtxAndPath(ArrayGTXValue(gtxArr), gtxPaths)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, gtxList)

    }

    fun buildTreeOf9(): TreeHolderFromArray {
        val intArray = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

        val expectedTree = "               +                               \n" +
                "              / \\               \n" +
                "             /   \\              \n" +
                "            /     \\             \n" +
                "           /       \\            \n" +
                "          /         \\           \n" +
                "         /           \\          \n" +
                "        /             \\         \n" +
                "       /               \\        \n" +
                "       +               9               \n" +
                "      / \\                       \n" +
                "     /   \\                      \n" +
                "    /     \\                     \n" +
                "   /       \\                    \n" +
                "   +       +       .       .       \n" +
                "  / \\     / \\                   \n" +
                " /   \\   /   \\                  \n" +
                " +   +   +   +   .   .   .   .   \n" +
                "/ \\ / \\ / \\ / \\                 \n" +
                "1 2 3 4 5 6 7 8 - - - - - - - - "


        val gtxList: List<GTXValue> = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        val fullBinaryTree: GtxBinaryTree = factory.buildFromGtx(TreeHelper.transformGTXsToArrayGTXValue(gtxList))

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, gtxList)

    }

    fun buildTreeOf7WithSubTree(): TreeHolderFromArray {
        return buildTreeOf7(null)
    }

    fun buildTreeOf7WithSubTree(gtxPath: GTXPath): TreeHolderSubTree {
        val intArray = intArrayOf(1,2,3,4,5,6,7)
        val expectedTree =
                "                               +                                                               \n" +
                        "                              / \\                               \n" +
                        "                             /   \\                              \n" +
                        "                            /     \\                             \n" +
                        "                           /       \\                            \n" +
                        "                          /         \\                           \n" +
                        "                         /           \\                          \n" +
                        "                        /             \\                         \n" +
                        "                       /               \\                        \n" +
                        "                      /                 \\                       \n" +
                        "                     /                   \\                      \n" +
                        "                    /                     \\                     \n" +
                        "                   /                       \\                    \n" +
                        "                  /                         \\                   \n" +
                        "                 /                           \\                  \n" +
                        "                /                             \\                 \n" +
                        "               /                               \\                \n" +
                        "               +                               +                               \n" +
                        "              / \\                             / \\               \n" +
                        "             /   \\                           /   \\              \n" +
                        "            /     \\                         /     \\             \n" +
                        "           /       \\                       /       \\            \n" +
                        "          /         \\                     /         \\           \n" +
                        "         /           \\                   /           \\          \n" +
                        "        /             \\                 /             \\         \n" +
                        "       /               \\               /               \\        \n" +
                        "       +               +               +               7               \n" +
                        "      / \\             / \\             / \\                       \n" +
                        "     /   \\           /   \\           /   \\                      \n" +
                        "    /     \\         /     \\         /     \\                     \n" +
                        "   /       \\       /       \\       /       \\                    \n" +
                        "   1       2       3       +       5       6       .       .       \n" +
                        "                          / \\                                   \n" +
                        "                         /   \\                                  \n" +
                        " .   .   .   .   .   .   +   3   .   .   .   .   .   .   .   .   \n" +
                        "                        / \\                                     \n" +
                        "- - - - - - - - - - - - 1 9 - - - - - - - - - - - - - - - - - - "


        val gtxArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        // Add the inner ArrayGtxValue
        val innerIntArray = intArrayOf(1,9,3)
        val innerIntArrayList = TreeHelper.transformIntToGTXValue(innerIntArray.toCollection(ArrayList()))
        val innerGtxIntArray: Array<GTXValue> = innerIntArrayList.toTypedArray()
        val innerArrayGTXValue = ArrayGTXValue(innerGtxIntArray)
        gtxArrayList.set(3, innerArrayGTXValue)

        val gtxArr: Array<GTXValue> = gtxArrayList.toTypedArray()

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(ArrayGTXValue(gtxArr), listOf(gtxPath))
        } else {
            factory.buildFromGtx(ArrayGTXValue(gtxArr))
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderSubTree(intArray, fullBinaryTree, treePrintout, expectedTree, gtxArrayList, innerGtxIntArray)
    }


}