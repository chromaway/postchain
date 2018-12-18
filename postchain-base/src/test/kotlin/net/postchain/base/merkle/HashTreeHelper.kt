package net.postchain.base.merkle

object HashTreeHelper {

    private val factory = HashFullBinaryTreeFactory()


    /*
    fun buildTreeOf4(): TreeHashHolderFromArray {
        val intArray = intArrayOf(1,2,3,4)
        val strArray = arrayOf("0102","0103","0104","0105")
        val expectedTree =
                "   +       \n" +
                        "  / \\   \n" +
                        " /   \\  \n" +
                        " +   +   \n" +
                        "/ \\ / \\ \n" +
                        "01 02 03 04 \n"

        val listOfHashes: List<Hash> = ArrayList<Hash>()
        val list: List<String> = intArray.toCollection(ArrayList())
        val hashArrayList = TreeHelper.transformIntToHash(list)

        val fullBinaryTree: HashFullBinaryTree = factory.buildFromArrayList(hashArrayList)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromHashTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)

        return TreeHashHolderFromArray(intArray, fullBinaryTree, treePrintout, expectedTree, hashArrayList)
    }
    */

}