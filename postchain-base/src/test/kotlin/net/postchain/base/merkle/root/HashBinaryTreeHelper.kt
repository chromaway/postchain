package net.postchain.base.merkle.root

import net.postchain.base.merkle.*

object HashBinaryTreeHelper {

    private val factory = HashBinaryTreeFactory()


    fun buildTreeOf1(): TreeHashHolderFromArray {
        val intArray = intArrayOf(1)
        val strArray = arrayOf("0102")
        val expectedTree =
                " +   \n" +
                "/ \\ \n" +
                "0102 - "

        val listOfHashes: List<Hash> = strArray.map { TreeHelper.convertToByteArray(it) }
        val fullBinaryTree: HashBinaryTree = factory.buildFromList(listOfHashes)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromHashTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHashHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, listOfHashes)
    }
    fun buildTreeOf4(): TreeHashHolderFromArray {
        val intArray = intArrayOf(1,2,3,4)
        val strArray = arrayOf("0102","0103","0104","0105")
        val expectedTree =
                "   +       \n" +
                "  / \\   \n" +
                " /   \\  \n" +
                " +   +   \n" +
                "/ \\ / \\ \n" +
                "0102 0103 0104 0105 \n"


        val listOfHashes: List<Hash> = strArray.map { TreeHelper.convertToByteArray(it) }
        val fullBinaryTree: HashBinaryTree = factory.buildFromList(listOfHashes)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromHashTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHashHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, listOfHashes)
    }

}