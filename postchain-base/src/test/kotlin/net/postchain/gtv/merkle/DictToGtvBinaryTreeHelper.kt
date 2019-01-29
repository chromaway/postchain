package net.postchain.gtv.merkle

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvPath
import net.postchain.gtv.GtvPathSet
import net.postchain.gtv.Gtv

object DictToGtvBinaryTreeHelper {

    private val factory =GtvBinaryTreeFactory()

    /**
     * When we only have one element in the Dict we don't have to generate dummies, since a dict will always have even pairs.
     */
    fun buildThreeOf1_fromDict(): TreeHolderFromDict {
        return buildThreeOf1_fromDict(null)
    }

    fun buildThreeOf1_fromDict(gtvPath:GtvPath?): TreeHolderFromDict {
        val stringArray = arrayOf("one")
        val intArray = intArrayOf(1)
        val expectedTree =
                " +   \n" +
                "/ \\ \n" +
                "one 1"


        val gtvDict =GtvTreeHelper.transformStringAndIntToGtvDictionary(stringArray.toCollection(ArrayList()), intArray.toCollection(ArrayList()))

        val fullBinaryTree:GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(gtvDict,GtvPathSet(setOf(gtvPath)))
        } else {
            factory.buildFromGtv(gtvDict)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return TreeHolderFromDict(intArray, fullBinaryTree, treePrintout, expectedTree,gtvDict)
    }

    fun buildThreeOf4_fromDict(): TreeHolderFromDict {
        return buildThreeOf4_fromDict(null)
    }


    fun buildThreeOf4_fromDict(gtvPath:GtvPath?): TreeHolderFromDict {
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


        val gtvDict =GtvTreeHelper.transformStringAndIntToGtvDictionary(stringArray.toCollection(ArrayList()), intArray.toCollection(ArrayList()))

        val fullBinaryTree:GtvBinaryTree = if (gtvPath != null) {
            factory.buildFromGtvAndPath(gtvDict,GtvPathSet(setOf(gtvPath)))
        } else {
            factory.buildFromGtv(gtvDict)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return TreeHolderFromDict(intArray, fullBinaryTree, treePrintout, expectedTree, gtvDict)
    }

    /**
     * Dict within a dict
     */
    fun buildTreeOf1WithSubTree(): TreeHolderFromDict {
        return buildTreeOf1WithSubTree(null)
    }

    fun buildTreeOf1WithSubTree(gtvPath:GtvPath?): TreeHolderFromDict {
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


        // Add the inner GtvDictionary
        val innerStringArray = arrayOf("seven", "eight")
        val innerIntArray = intArrayOf(7, 8)
        val innerGtvDict =GtvTreeHelper.transformStringAndIntToGtvDictionary(innerStringArray.toCollection(ArrayList()), innerIntArray.toCollection(ArrayList()))

        // Put the inner Dict in the outer Dict
        val outerMap = HashMap<String, Gtv>()
        outerMap.set("one", innerGtvDict)
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