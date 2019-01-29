package net.postchain.gtv.merkle

import net.postchain.base.merkle.*
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvPath
import net.postchain.gtv.GtvPathSet
import net.postchain.gtv.Gtv

object ArrayToGtvBinaryTreeHelper {


    private val factory =GtvBinaryTreeFactory()


    /**
     * Use this if you don't have a path to prove
     */
    fun buildTreeOf1(): TreeHolderFromArray {
        return buildTreeOf1(null)
    }

    fun buildTreeOf1(gtvPath: GtvPath?): TreeHolderFromArray {
        val intArray = intArrayOf(1)
        val expectedTree =
                " +   \n" +
                "/ \\ \n" +
                "1 - "

        val gtvArrayList = GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))
        val gtvArr: Array<Gtv> = gtvArrayList.toTypedArray()

        val fullBinaryTree: GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(GtvArray(gtvArr), GtvPathSet(setOf(gtvPath)))
        } else {
            factory.buildFromGtv(GtvArray(gtvArr))
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, gtvArrayList)
    }


    fun buildTreeOf4(): TreeHolderFromArray {
        return buildTreeOf4(null)
    }

    fun buildTreeOf4(gtvPath: GtvPath?): TreeHolderFromArray {
        val intArray = intArrayOf(1,2,3,4)
        val expectedTree =
                "   +       \n" +
                        "  / \\   \n" +
                        " /   \\  \n" +
                        " +   +   \n" +
                        "/ \\ / \\ \n" +
                        "1 2 3 4 \n"

        val gtvArrayList =GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))
        val gtvArr: Array<Gtv> = gtvArrayList.toTypedArray()
        val fullBinaryTree:GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(GtvArray(gtvArr), GtvPathSet(setOf((gtvPath))))
        } else {
            factory.buildFromGtv(GtvArray(gtvArr))
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, gtvArrayList)
    }

    fun buildTreeOf7(): TreeHolderFromArray {
        return buildTreeOf7(null)
    }

    fun buildTreeOf7(gtvPath: GtvPath?): TreeHolderFromArray {
        return if (gtvPath == null) {
            buildTreeOf7(GtvPathSet(setOf()))
        } else {
            buildTreeOf7(GtvPathSet(setOf(gtvPath)))
        }
    }

    fun buildTreeOf7(gtvPaths: GtvPathSet): TreeHolderFromArray {
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



        val gtvList: List<Gtv> =GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))
        val gtvArr: Array<Gtv> =gtvList.toTypedArray()

        val fullBinaryTree = factory.buildFromGtvAndPath(GtvArray(gtvArr),gtvPaths)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree,gtvList)

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


        val gtvList: List<Gtv> =GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))

        val fullBinaryTree: GtvBinaryTree = factory.buildFromGtv(GtvTreeHelper.transformGtvsToGtvArray(gtvList))

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, gtvList)

    }

    fun buildTreeOf7WithSubTree(): TreeHolderFromArray {
        return buildTreeOf7(null)
    }

    fun buildTreeOf7WithSubTree(gtvPath: GtvPath): TreeHolderSubTree {
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


        val gtvArrayList =GtvTreeHelper.transformIntToGtv(intArray.toCollection(ArrayList()))

        // Add the inner GtvArray
        val innerIntArray = intArrayOf(1,9,3)
        val innerIntArrayList =GtvTreeHelper.transformIntToGtv(innerIntArray.toCollection(ArrayList()))
        val innerGtvIntArray: Array<Gtv> = innerIntArrayList.toTypedArray()
        val innerGtvArray = GtvArray(innerGtvIntArray)
        gtvArrayList.set(3, innerGtvArray)

        val gtvArr: Array<Gtv> = gtvArrayList.toTypedArray()

        val fullBinaryTree: GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(GtvArray(gtvArr), GtvPathSet(setOf(gtvPath)))
        } else {
            factory.buildFromGtv(GtvArray(gtvArr))
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHolderSubTree(intArray, fullBinaryTree, treePrintout, expectedTree, gtvArrayList, innerGtvIntArray)
    }


}