package net.postchain.gtx.merkle

import net.postchain.base.merkle.*
import net.postchain.core.UserMistake
import net.postchain.gtx.*
import java.util.SortedSet


/**
 * This can build two types of trees:
 * 1. Make a binary tree out of a GTX object graph
 * 2. Same as above, but we also marked each GTX sub structure that should be a path leaf.
 *    If you want this option (2) you have to provide a list of [GTXPath]
 */
class GtxBinaryTreeFactory : BinaryTreeFactory<GTXValue, GTXPath>() {

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
    fun buildFromGtxAndPath(gtxValue: GTXValue, gtxPathList: List<GTXPath>): GtxBinaryTree {
        val result = handleLeaf(gtxValue, gtxPathList)
        return GtxBinaryTree(result)
    }

    // ------------ Internal ----------

    /**
     * There are 2 edge cases here:
     * - When the args is empty. -> We return a top node with two empty leafs
     * - When there is only one element. -> We set the right element as empty
     */
    private fun buildFromArrayGTXValue(arrayGTXValue: ArrayGTXValue, gtxPathList: List<GTXPath>): BinaryTreeElement {
        val isThisAProofLeaf = gtxPathList.any{ it.isAtLeaf() } // Will tell us if any of the paths points to this element
        //println("Arr,(is proof? $isThisAProofLeaf) Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))

        // 1. Build leaf layer
        val leafList: List<GTXValue> = arrayGTXValue.array.map {it}
        if (leafList.isEmpty()) {
            return GtxArrayHeadNode(EmptyLeaf, EmptyLeaf, isThisAProofLeaf, arrayGTXValue, 0)
        }

        val leafArray = buildLeafElements(leafList, gtxPathList)

        // 2. Build all higher layers
        val result = buildHigherLayer(1, leafArray)

        // 3. Fix and return the root node
        val orgRoot = result.get(0)
        return when (orgRoot) {
            is Node -> {
                GtxArrayHeadNode(orgRoot.left, orgRoot.right, isThisAProofLeaf, arrayGTXValue, leafList.size)
            }
            is Leaf<*> -> {
                if (leafList.size > 1) {
                    throw IllegalStateException("How come we got a leaf returned when we had ${leafList.size} elements is the args?")
                } else {
                    // Create a dummy to the right
                    GtxArrayHeadNode(orgRoot, EmptyLeaf, isThisAProofLeaf, arrayGTXValue, leafList.size)
                }
            }
            else -> throw IllegalStateException("Should not find element of this type here: $orgRoot")
        }
    }

    /**
     * The strategy for transforming [DictGTXValue] is pretty simple, we treat the key and value as leafs and
     * add them both as elements to the tree.
     *
     * There is an edge cases here:
     * - When the dict is empty. -> We return a top node with two empty leafs
     */
    private fun buildFromDictGTXValue(dictGTXValue: DictGTXValue, gtxPathList: List<GTXPath>): GtxDictHeadNode {
        val isThisAProofLeaf = gtxPathList.any{ it.isAtLeaf() } // Will tell us if any of the paths points to this element
        //println("Dict,(is proof? $isThisAProofLeaf) Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))
        val keys: SortedSet<String> = dictGTXValue.dict.keys.toSortedSet() // Needs to be sorted, or else the order is undefined

        if (keys.isEmpty()) {
            return GtxDictHeadNode(EmptyLeaf, EmptyLeaf, isThisAProofLeaf, dictGTXValue, keys.size)
        }

        val leafArray = arrayListOf<BinaryTreeElement>()

        // 1. Build first (leaf) layer
        for (key in keys) {

            //println("key extracted: $key")

            // 1.a Fix the key
            val keyGtxString: GTXValue = StringGTXValue(key)
            val keyElement = handleLeaf(keyGtxString, GTXPath.NO_PATHS) // The key cannot not be proved, so NO_PATHS
            leafArray.add(keyElement)

            // 1.b Fix the value/content
            val pathsRelevantForThisLeaf = GTXPath.getTailIfFirstElementIsDictOfThisKeyFromList(key, gtxPathList)
            val content: GTXValue = dictGTXValue.get(key)!!  // TODO: Is it ok to bang here if the dict is broken?
            val contentElement = handleLeaf(content, pathsRelevantForThisLeaf)
            leafArray.add(contentElement)
        }

        // 2. Build all higher layers
        val result = buildHigherLayer(1, leafArray)

        // 3. Fix and return the root node
        val orgRoot = result.get(0)
        return when (orgRoot) {
            is Node -> GtxDictHeadNode(orgRoot.left, orgRoot.right, isThisAProofLeaf, dictGTXValue, keys.size)
            else -> throw IllegalStateException("Should not find element of this type here: $orgRoot")
        }
    }


    /**
     * Design: We add the operation name first in the argument array, and just use same algorithm as for array
     *
     * There are 2 edge cases here:
     * - When there is no operation name. -> Bang out! Not allowed
     * - When there are no arguments. -> We set the right element as empty
     */
    private fun buildFromOperationGTXValue(operationGTXValue: OperationGTXValue, gtxPathList: List<GTXPath>): BinaryTreeElement {
        val isThisAProofLeaf = gtxPathList.any{ it.isAtLeaf() } // Will tell us if any of the paths points to this element
        //println("Arr,(is proof? $isThisAProofLeaf) Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))

        // 1. Build leaf layer
        val leafList: MutableList<GTXValue> = mutableListOf(operationGTXValue.name)
        for (arg: GTXValue in operationGTXValue.args.asArray()) {
            leafList.add(arg)
        }

        val leafArray = buildLeafElements(leafList, gtxPathList)

        // 2. Build all higher layers
        val result = buildHigherLayer(1, leafArray)

        // 3. Fix and return the root node
        val orgRoot = result.get(0)
        return when (orgRoot) {
            is Node -> {
                GtxOperationHeadNode(orgRoot.left, orgRoot.right, isThisAProofLeaf, operationGTXValue, leafList.size)
            }
            is Leaf<*> -> {
                if (leafList.size > 1) {
                    throw IllegalStateException("How come we got a leaf returned when we had ${leafList.size} elements is the args?")
                } else {
                    // No argument operation is OK. Create a dummy to the right
                    GtxOperationHeadNode(orgRoot, EmptyLeaf, isThisAProofLeaf, operationGTXValue, leafList.size)
                }
            }
            else -> throw IllegalStateException("Should not find element of this type here: $orgRoot")
        }
    }


    /**
     * Design:
     *   Left: Put the operations on this side,
     *   Right: We add the blockchain RID first, then we add the signers (if any)
     * ... and just use same algorithm as for array for both sides separately
     *
     * There are 3 edge cases here:
     * - When there are no operations -> Bang out! Not allowed
     * - When there is no blockchainRID -> Bang out! Not allowed
     * - When there are no signers. -> Allowed
     */
    private fun buildFromTransactionBodyGTXValue(transactionBodyGTXValue: TransactionBodyGTXValue, gtxPathList: List<GTXPath>): BinaryTreeElement {
        val isThisAProofLeaf = gtxPathList.any{ it.isAtLeaf() } // Will tell us if any of the paths points to this element
        //println("Arr,(is proof? $isThisAProofLeaf) Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))

        // ----- Left side (Operations) ---
        if (transactionBodyGTXValue.operations.isEmpty()) {
            throw UserMistake("Cannot have a transaction without operations!")
        }

        // 1. Build leaf layer
        val leftLeafs: List<BinaryTreeElement> = transactionBodyGTXValue.operations.map { buildFromOperationGTXValue(it, gtxPathList) }

        // 2. Build all higher layers
        val leftResult = buildHigherLayer(1, leftLeafs)

        // 3. Fix and return the root node
        val leftRoot = leftResult.get(0) // It doesn't matter if this is a "node" or a "leaf", we can use it anyway

        // ----- Right side (BlockchainRID + signers) ---
        // 1. Build leaf layer
        val rightLeafList: MutableList<GTXValue> = mutableListOf(transactionBodyGTXValue.blockchainRid)
        for (signer: GTXValue in transactionBodyGTXValue.signers) {
            rightLeafList.add(signer)
        }

        val rightLeafs = buildLeafElements(rightLeafList, gtxPathList)

        // 2. Build all higher layers
        val rightResult = buildHigherLayer(1, rightLeafs)

        // 3. Fix and return the root node
        val rightRoot = rightResult.get(0) // It doesn't matter if this is a "node" or a "leaf", we can use it anyway

        // ----- Wrap it up ----------
        return GtxTransBodyHeadNode(leftRoot, rightRoot, isThisAProofLeaf, transactionBodyGTXValue, leftLeafs.size) // Here we use size to remember how many operations we have
    }

    /**
     * Design:
     *   Left: Put the transaction body here
     *   Right: We add the signatures (if any)
     * ... and just use same algorithm as for array for both sides separately
     *
     * There are 2 edge cases here:
     * - When there is no transaction body -> Bang out! Not allowed
     * - When there are no signatures. -> Allowed
     */
    private fun buildFromTransactionGTXValue(transactionGTXValue: TransactionGTXValue, gtxPathList: List<GTXPath>): BinaryTreeElement {
        val isThisAProofLeaf = gtxPathList.any{ it.isAtLeaf() } // Will tell us if any of the paths points to this element
        //println("Arr,(is proof? $isThisAProofLeaf) Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))

        // ----- Left side (Transaction Body) ---
        val leftRoot = buildFromTransactionBodyGTXValue(transactionGTXValue.transactionBody, gtxPathList)

        // ----- Right side (signatures) ---
        val rightRoot = if (transactionGTXValue.signatures.isEmpty()) {
            EmptyLeaf // This is acceptable, but we have to handle it with a dummy
        } else {
            // 1. Build leaf layer
            val rightLeafList: List<GTXValue> = transactionGTXValue.signatures.map {it}

            val rightLeafs = buildLeafElements(rightLeafList, gtxPathList)

            // 2. Build all higher layers
            val rightResult = buildHigherLayer(1, rightLeafs)

            // 3. Return the root node
            rightResult.get(0) // Doesn't matter if we have one or many ("node"/"leaf"), just use it
        }

        // ----- Wrap it up ----------
        return GtxTransHeadNode(leftRoot, rightRoot, isThisAProofLeaf, transactionGTXValue, transactionGTXValue.signatures.size) // Here we use size to remember how many signatures we have
    }

    /**
     * @param leafList the list of [GTXValue] we will use for leafs in the tree
     * @param gtxPathList the paths we have to consider while creating the leafs
     * @return an array of all the leafs as [BinaryTreeElement] s. Note that some leafs might not be primitive values
     *   but some sort of collection with their own leafs (recursivly)
     */
    private fun buildLeafElements(leafList: List<GTXValue>, gtxPathList: List<GTXPath>): ArrayList<BinaryTreeElement> {
        val leafArray = arrayListOf<BinaryTreeElement>()

        // 1. Build first (leaf) layer
        for (i in 0..(leafList.size - 1)) {
            val pathsRelevantForThisLeaf = GTXPath.getTailIfFirstElementIsArrayOfThisIndexFromList(i, gtxPathList)
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
    override fun handleLeaf(leaf: GTXValue, gtxPathList: List<GTXPath>): BinaryTreeElement {
        //println("handleLeaf, Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))
        return when (leaf) {
            is ArrayGTXValue -> buildFromArrayGTXValue(leaf, gtxPathList)
            is DictGTXValue -> buildFromDictGTXValue(leaf, gtxPathList)
            is OperationGTXValue -> buildFromOperationGTXValue(leaf, gtxPathList)
            is TransactionBodyGTXValue -> buildFromTransactionBodyGTXValue(leaf, gtxPathList)
            is TransactionGTXValue -> buildFromTransactionGTXValue(leaf, gtxPathList)
            else -> {
                val isThisAProofLeaf = gtxPathList.any{ it.isAtLeaf() } // Will tell us if any of the paths points to this element
                //println("GTX leaf, proof? $isThisAProofLeaf")
                Leaf(leaf, isThisAProofLeaf)
            }
        }
    }
}