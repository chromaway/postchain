package net.postchain.gtx.merkle.factory

import net.postchain.base.merkle.BinaryTreeElement
import net.postchain.base.merkle.EmptyLeaf
import net.postchain.core.UserMistake
import net.postchain.gtx.GTXPathSet
import net.postchain.gtx.GTXValue
import net.postchain.gtx.TransactionBodyGTXValue
import net.postchain.gtx.TransactionGTXValue
import net.postchain.gtx.merkle.GtxBinaryTreeFactory
import net.postchain.gtx.merkle.GtxTransBodyHeadNode
import net.postchain.gtx.merkle.GtxTransHeadNode

object GtxBinaryTreeFactoryTransaction {


    private val mainFactory = GtxBinaryTreeFactory()

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
    fun buildFromTransactionBodyGTXValue(transactionBodyGTXValue: TransactionBodyGTXValue, gtxPaths: GTXPathSet): BinaryTreeElement {
        val isThisAProofLeaf = gtxPaths.isThisAProofLeaf()  // Will tell us if any of the paths points to this element
        //println("Arr,(is proof? $isThisAProofLeaf) Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))

        // ----- Left side (Operations) ---
        if (transactionBodyGTXValue.operations.isEmpty()) {
            throw UserMistake("Cannot have a transaction without operations!")
        }

        // 1. Build leaf layer
        val onlyOperationPaths = gtxPaths.keepOnlyPathsToOperations()
        val leftLeafs: List<BinaryTreeElement> = transactionBodyGTXValue.operations.map { GtxBinaryTreeFactoryOperation.buildFromOperationGTXValue(it, onlyOperationPaths) }

        // 2. Build all higher layers
        val leftResult = mainFactory.buildHigherLayer(1, leftLeafs)

        // 3. Fix and return the root node
        val leftRoot = leftResult.get(0) // It doesn't matter if this is a "node" or a "leaf", we can use it anyway

        // ----- Right side (BlockchainRID + signers) ---
        // 1. Build leaf layer
        val rightLeafList: MutableList<GTXValue> = mutableListOf(transactionBodyGTXValue.blockchainRid)
        for (signer: GTXValue in transactionBodyGTXValue.signers) {
            rightLeafList.add(signer)
        }

        val rightLeafs = buildLeafElementsFromSigners(rightLeafList, gtxPaths)

        // 2. Build all higher layers
        val rightResult = mainFactory.buildHigherLayer(1, rightLeafs)

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
    fun buildFromTransactionGTXValue(transactionGTXValue: TransactionGTXValue, gtxPaths: GTXPathSet): BinaryTreeElement {
        val isThisAProofLeaf = gtxPaths.isThisAProofLeaf() // Will tell us if any of the paths points to this element
        //println("Arr,(is proof? $isThisAProofLeaf) Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))

        // ----- Left side (Transaction Body) ---
        val leftRoot = buildFromTransactionBodyGTXValue(transactionGTXValue.transactionBody, gtxPaths)

        // ----- Right side (signatures) ---
        val rightRoot = if (transactionGTXValue.signatures.isEmpty()) {
            EmptyLeaf // This is acceptable, but we have to handle it with a dummy
        } else {
            // 1. Build leaf layer
            val rightLeafList: List<GTXValue> = transactionGTXValue.signatures.map {it}


            val rightLeafs = buildLeafElementsFromSignatures(rightLeafList, gtxPaths)

            // 2. Build all higher layers
            val rightResult = mainFactory.buildHigherLayer(1, rightLeafs)

            // 3. Return the root node
            rightResult.get(0) // Doesn't matter if we have one or many ("node"/"leaf"), just use it
        }

        // ----- Wrap it up ----------
        return GtxTransHeadNode(leftRoot, rightRoot, isThisAProofLeaf, transactionGTXValue, transactionGTXValue.signatures.size) // Here we use size to remember how many signatures we have
    }

    /**
     * When we have the signer list it is a bit more complicated, since we have put meta data at pos = 0
     *
     *    WARNING: potential off-by-one error, see impl below
     *
     * @param signerList the list of [GTXValue] representing signers
     * @param gtxPathList the paths we have to consider while creating the leafs
     * @return an array of all the leafs as [BinaryTreeElement] s.
     */
    private fun buildLeafElementsFromSigners(signerList: List<GTXValue>, gtxPaths: GTXPathSet): ArrayList<BinaryTreeElement> {
        val leafArray = arrayListOf<BinaryTreeElement>()

        val onlySignerPaths: GTXPathSet = gtxPaths.keepOnlyPathsToSigners()

        for (i in 0..(signerList.size - 1)) {
            val pathsRelevantForThisLeaf = onlySignerPaths.getTailIfFirstElementIsArrayOfThisIndexFromList(i + 1) // <---------- NOTE the +1
            //println("New paths, (size: ${pathsRelevantForThisLeaf.size} ) list: " + GTXPath.debugRerpresentation(pathsRelevantForThisLeaf))
            val leaf = signerList[i]
            val binaryTreeElement = mainFactory.handlePrimitiveLeaf(leaf, pathsRelevantForThisLeaf)
            leafArray.add(binaryTreeElement)
        }
        return leafArray
    }

    /**
     * Transforms a bunch of signatures into [BinaryTreeElement] s.
     */
    private fun buildLeafElementsFromSignatures(signatureLeafList: List<GTXValue>, gtxPaths: GTXPathSet): ArrayList<BinaryTreeElement> {
        val filteredGtxPaths = gtxPaths.keepOnlyPathsToSignatures()
        return mainFactory.buildLeafElements(signatureLeafList, filteredGtxPaths)
    }
}