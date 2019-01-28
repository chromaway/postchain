package net.postchain.gtx.merkle

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreeHelper
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtx.*

object TransactionBodyToGtxBinaryTreeHelper {


    private val factory = GtxBinaryTreeFactory()


    fun buildTransBodyGtxValueOneOperation(blockchainRidHex: String, signersHex: List<String>): TransactionBodyGTXValue {
        val bcRid  = ByteArrayGTXValue(TreeHelper.convertToByteArray(blockchainRidHex))
        val operation1: OperationGTXValue = OperationToGtxBinaryTreeHelper.buildOnlyNameOperation("ZOp")
        val signers: Array<ByteArrayGTXValue> = signersHex.map{ ByteArrayGTXValue(TreeHelper.convertToByteArray(it)) }.toTypedArray()
        return TransactionBodyGTXValue(bcRid, arrayOf(operation1), signers)
    }

    fun buildTransBodyGtxValueTwoOperation(blockchainRidHex: String, signersHex: List<String>): TransactionBodyGTXValue {
        val bcRid  = ByteArrayGTXValue(TreeHelper.convertToByteArray(blockchainRidHex))
        val operation1: OperationGTXValue = OperationToGtxBinaryTreeHelper.buildOperationWithIntegerArugments("MyOp", intArrayOf(1,2,3,4))
        val operation2: OperationGTXValue = OperationToGtxBinaryTreeHelper.buildOnlyNameOperation("ZOp")
        val signers: Array<ByteArrayGTXValue> = signersHex.map{ ByteArrayGTXValue(TreeHelper.convertToByteArray(it)) }.toTypedArray()
        return TransactionBodyGTXValue(bcRid, arrayOf(operation1, operation2), signers)
    }

    // -------------- 0 signer body -------------
    /**
     * Use this if you don't have a path to prove
     */
    fun buildTreeOfBodyWith1Operation_andNoSigners(): TreeHolderFromTransactionBody {
        return buildTreeOfBodyWith1Operation_andNoSigners(null)
    }

    fun buildTreeOfBodyWith1Operation_andNoSigners(gtxPath: GTXPath?): TreeHolderFromTransactionBody {
        val blockchainRidStr = "FF00FF00"
        val intArray = intArrayOf()
        val expectedTree = "   +       \n" +
                "  / \\   \n" +
                " /   \\  \n" +
                " +   FF00FF00   \n" +
                "/ \\     \n" +
                "ZOp - - - "

        val transBody =  buildTransBodyGtxValueOneOperation(blockchainRidStr, listOf())

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(transBody, GTXPathSet(setOf(gtxPath)))
        } else {
            factory.buildFromGtx(transBody)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)

        return TreeHolderFromTransactionBody(intArray, fullBinaryTree, treePrintout, expectedTree, transBody)
    }


    // -------------- 1 signer body -------------
    fun buildTreeOfBodyWith2Operations_andASigner(): TreeHolderFromTransactionBody {
        return buildTreeOfBodyWith2Operations_andASigner(null)
    }

    fun buildTreeOfBodyWith2Operations_andASigner(gtxPath: GTXPath?): TreeHolderFromTransactionBody {
        val blockchainRidStr = "FF00FF00"
        val signer1 = "22112211"
        val intArray = intArrayOf()
        val expectedTree = "                               +                                                               \n" +
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
                "       +               +               FF00FF00               22112211               \n" +
                "      / \\             / \\                                       \n" +
                "     /   \\           /   \\                                      \n" +
                "    /     \\         /     \\                                     \n" +
                "   /       \\       /       \\                                    \n" +
                "   +       4       ZOp       -       .       .       .       .       \n" +
                "  / \\                                                           \n" +
                " /   \\                                                          \n" +
                " +   +   .   .   .   .   .   .   .   .   .   .   .   .   \n" +
                "/ \\ / \\                                                 \n" +
                "MyOp 1 2 3 - - - - - - - - - - - - - - - - - - - - - - - - "

        val transBody =  buildTransBodyGtxValueTwoOperation(blockchainRidStr, listOf(signer1))

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(transBody, GTXPathSet(setOf(gtxPath)))
        } else {
            factory.buildFromGtx(transBody)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)

        return TreeHolderFromTransactionBody(intArray, fullBinaryTree, treePrintout, expectedTree, transBody)
    }
}