package net.postchain.gtx.merkle

import net.postchain.base.merkle.*
import net.postchain.gtx.*
import net.postchain.gtx.merkle.factory.GtxBinaryTreeFactoryArray
import net.postchain.gtx.merkle.factory.GtxBinaryTreeFactoryDict
import net.postchain.gtx.merkle.factory.GtxBinaryTreeFactoryOperation
import net.postchain.gtx.merkle.factory.GtxBinaryTreeFactoryTransaction


/**
 * This can build two types of trees:
 * 1. Make a binary tree out of a GTX object graph
 * 2. Same as above, but we also marked each GTX sub structure that should be a path leaf.
 *    If you want this option (2) you have to provide a list of [GTXPath]
 */
class GtxBinaryTreeFactory : BinaryTreeFactory<GTXValue, GTXPathSet>() {

    /**
     * Generic builder.
     * @param gtxValue will take any damn thing
     */
    fun buildFromGtx(gtxValue: GTXValue): GtxBinaryTree {
        return buildFromGtxAndPath(gtxValue, GTXPath.NO_PATHS)
    }

    /**
     * Generic builder.
     * @param gtxValue will take any damn thing
     * @param gtxPathList will tell us what element that are path leafs
     */
    fun buildFromGtxAndPath(gtxValue: GTXValue, gtxPaths: GTXPathSet): GtxBinaryTree {
        val result = handleLeaf(gtxValue, gtxPaths)
        return GtxBinaryTree(result)
    }


    /**
     * The generic method that builds [BinaryTreeElement] from [GTXValue] s.
     * The only tricky bit of this method is that we need to remove paths that are irrelevant for the leaf in question.
     *
     * @param leafList the list of [GTXValue] we will use for leafs in the tree
     * @param gtxPaths the paths we have to consider while creating the leafs
     * @return an array of all the leafs as [BinaryTreeElement] s. Note that some leafs might not be primitive values
     *   but some sort of collection with their own leafs (recursivly)
     */
    fun buildLeafElements(leafList: List<GTXValue>, gtxPaths: GTXPathSet): ArrayList<BinaryTreeElement> {
        val leafArray = arrayListOf<BinaryTreeElement>()

        val onlyArrayPaths = gtxPaths.keepOnlyArrayPaths() // For performance, since we will loop soon

        for (i in 0..(leafList.size - 1)) {
            val pathsRelevantForThisLeaf = onlyArrayPaths.getTailIfFirstElementIsArrayOfThisIndexFromList(i)
            //println("New paths, (size: ${pathsRelevantForThisLeaf.size} ) list: " + GTXPath.debugRerpresentation(pathsRelevantForThisLeaf))
            val leaf = leafList[i]
            val binaryTreeElement = handleLeaf(leaf, pathsRelevantForThisLeaf)
            leafArray.add(binaryTreeElement)
        }
        return leafArray
    }

    /**
     * Handles different types of [GTXValue] values
     */
    override fun handleLeaf(leaf: GTXValue, gtxPathSet: GTXPathSet?): BinaryTreeElement {
        val gtxPaths = if (gtxPathSet == null) {
            GTXPath.NO_PATHS
        } else {
            gtxPathSet
        }

        //println("handleLeaf, Proof path (size: ${gtxPaths.size} ) list: " + GTXPath.debugRerpresentation(gtxPaths))
        return when (leaf) {
            is ArrayGTXValue -> GtxBinaryTreeFactoryArray.buildFromArrayGTXValue(leaf, gtxPaths)
            is DictGTXValue -> GtxBinaryTreeFactoryDict.buildFromDictGTXValue(leaf, gtxPaths)
            is OperationGTXValue -> GtxBinaryTreeFactoryOperation.buildFromOperationGTXValue(leaf, gtxPaths)
            is TransactionBodyGTXValue -> GtxBinaryTreeFactoryTransaction.buildFromTransactionBodyGTXValue(leaf, gtxPaths)
            is TransactionGTXValue -> GtxBinaryTreeFactoryTransaction.buildFromTransactionGTXValue(leaf, gtxPaths)
            else -> {
                if (leaf.isContainerType()) {
                    throw IllegalStateException("Programmer should have dealt with this container type: ${leaf.type}")
                }
                handlePrimitiveLeaf(leaf, gtxPaths)
            }
        }
    }
}