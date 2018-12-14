package net.postchain.base.merkle

import net.postchain.gtx.*

object TreeHelper {

    /**
     * Transforms (readable) integers into [IntegerGTXValue]
     */
    fun transformIntToGTXValue(ints: List<Int>): MutableList<GTXValue> {
        val retList = arrayListOf<GTXValue>()
        for (i in ints) {
            retList.add(IntegerGTXValue(i.toLong()))
        }
        return retList
    }

    /**
     * Transforms (readable) strings and integers into a [DictGTXValue]
     */
    fun transformStringAndIntToDictGTXValue(strings: List<String>, ints: List<Int>): DictGTXValue {
        if (strings.size != ints.size) {
            throw IllegalArgumentException("Cannot make a Dict if we don't have equal amount of keys and content")
        }
        val dict = HashMap<String, GTXValue>()

        for (i in 0..(strings.size - 1)) {
            val key = strings[i]
            val content = IntegerGTXValue(ints[i].toLong())
            dict.set(key, content)
        }
        return DictGTXValue(dict)

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

    fun buildTreeOf4(): TreeHolderFromArray {
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

        return TreeHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, gtxArrayList)
    }

    fun buildTreeOf7(): TreeHolderFromArray {
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

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(gtxList)

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

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(gtxList)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, gtxList)

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

        return TreeHolderSubTree(intArray, fullBinaryTree, treePrintout, expectedTree, gtxArrayList, innerGtxIntArray)
    }

    fun buildThreeOf4_fromDict(): TreeHolderFromDict {
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

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(gtxDict)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return TreeHolderFromDict(intArray, fullBinaryTree, treePrintout, expectedTree, gtxDict)
    }
}