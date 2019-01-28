package net.postchain.gtx.merkle

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreePrinter
import net.postchain.gtx.*

object OperationToGtxBinaryTreeHelper {


    private val factory = GtxBinaryTreeFactory()

    /**
     * @return a dummy [OperationGTXValue] with a name but without arguments
     */
    fun buildOnlyNameOperation(nameStr: String): OperationGTXValue {
        val name = StringGTXValue(nameStr)
        val args = ArrayGTXValue(arrayOf())
        return OperationGTXValue(name, args)
    }

    /**
     * @return a dummy [OperationGTXValue] with a name and a number of all-integer arguments
     */
    fun buildOperationWithIntegerArugments(nameStr: String, args: IntArray): OperationGTXValue {
        val name = StringGTXValue(nameStr)

        val gtxArrayList = GtxTreeHelper.transformIntToGTXValue(args.toCollection(ArrayList()))
        val gtxArr: Array<GTXValue> = gtxArrayList.toTypedArray()
        val args = ArrayGTXValue(gtxArr)

        return OperationGTXValue(name, args)
    }

    // -------------- 0 argument operation -------------
    /**
     * Use this if you don't have a path to prove
     */
    fun buildTreeOfOnlyName(): TreeHolderFromOperation {
        return buildTreeOfOnlyName(null)
    }

    fun buildTreeOfOnlyName(gtxPath: GTXPath?): TreeHolderFromOperation {
        val nameStr = "ZOp"
        val intArray = intArrayOf()
        val expectedTree = " +   \n" +
                "/ \\ \n" +
                "ZOp -"

        val operation = buildOnlyNameOperation(nameStr)

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(operation, GTXPathSet(setOf(gtxPath)))
        } else {
            factory.buildFromGtx(operation)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)

        return TreeHolderFromOperation(intArray, fullBinaryTree, treePrintout, expectedTree, operation)
    }

    // -------------- 4 argument operation ----
    /**
     * Use this if you don't have a path to prove
     */
    fun buildTreeOf4Args(): TreeHolderFromOperation {
        return buildTreeOf4Args(null)
    }

    fun buildTreeOf4Args(gtxPath: GTXPath?): TreeHolderFromOperation {
        val nameStr = "MyOp"
        val intArray = intArrayOf(1,2,3,4)
        val expectedTree = "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       4       \n" +
                "  / \\           \n" +
                " /   \\          \n" +
                " +   +   .   .   \n" +
                "/ \\ / \\         \n" +
                "MyOp 1 2 3 - - - - "

        val operation = buildOperationWithIntegerArugments(nameStr, intArray)

        val fullBinaryTree: GtxBinaryTree = if (gtxPath != null) {
            factory.buildFromGtxAndPath(operation, GTXPathSet(setOf(gtxPath)))
        } else {
            factory.buildFromGtx(operation)
        }

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)

        return TreeHolderFromOperation(intArray, fullBinaryTree, treePrintout, expectedTree, operation)
    }
}