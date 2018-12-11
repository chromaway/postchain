package net.postchain.base.merkle

import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.GTXValue
import net.postchain.gtx.IntegerGTXValue

object TreeHelper {

    /**
     * Useful for transforming readable
     */
    fun transformIntToGTXValue(intArray: ArrayList<Int>): ArrayList<GTXValue> {
        val retArr = arrayListOf<GTXValue>()
        for (i in intArray) {
            retArr.add(IntegerGTXValue(i.toLong()))
        }
        return retArr
    }

    /**
     * @return A readable HEX string of the ByteArray
     */
    fun convertToHex(bytes: ByteArray): String {
        val sb: StringBuilder = StringBuilder()
        for (b in bytes) {
            val st = String.format("%02X", b)
            sb.append(st)
        }
        return sb.toString()
    }

    fun buildTreeOf4(): TreeHolder {
        val intArray = intArrayOf(1,2,3,4)
        val expectedTree =
                "   +       \n" +
                "  / \\   \n" +
                " /   \\  \n" +
                " +   +   \n" +
                "/ \\ / \\ \n" +
                "1 2 3 4 \n"

        val gtxArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(gtxArrayList)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolder(intArray, gtxArrayList, fullBinaryTree, treePrintout, expectedTree)
    }

    fun buildTreeOf7(): TreeHolder {
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



        val gtxArrayList: ArrayList<GTXValue> = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(gtxArrayList)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolder(intArray, gtxArrayList, fullBinaryTree, treePrintout, expectedTree)

    }

    fun buildTreeOf9(): TreeHolder {
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


        val gtxArrayList: ArrayList<GTXValue> = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(gtxArrayList)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolder(intArray, gtxArrayList, fullBinaryTree, treePrintout, expectedTree)

    }

    fun buildTreeOf7WithSubTree(): TreeHolderSubTree {
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

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(gtxArrayList)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderSubTree(intArray, gtxArrayList, fullBinaryTree, treePrintout, expectedTree, innerGtxIntArray)
    }
}