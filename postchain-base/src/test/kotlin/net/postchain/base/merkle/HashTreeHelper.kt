package net.postchain.base.merkle

object HashTreeHelper {

    private val factory = HashFullBinaryTreeFactory()


    fun buildTreeOf4(): TreeHashHolderFromArray {
        val intArray = intArrayOf(1,2,3,4)
        val expectedTree =
                "   +       \n" +
                        "  / \\   \n" +
                        " /   \\  \n" +
                        " +   +   \n" +
                        "/ \\ / \\ \n" +
                        "01 02 03 04 \n"

        val hashArrayList = TreeHelper.transformIntToHash(intArray.toCollection(ArrayList()))

        val fullBinaryTree: HashFullBinaryTree = factory.buildFromArrayList(hashArrayList)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromHashTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHashHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, hashArrayList)
    }

}