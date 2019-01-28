package net.postchain.gtx.merkle.factory

import net.postchain.base.merkle.BinaryTreeElement
import net.postchain.base.merkle.EmptyLeaf
import net.postchain.base.merkle.Leaf
import net.postchain.base.merkle.Node
import net.postchain.gtx.GTXPathSet
import net.postchain.gtx.GTXValue
import net.postchain.gtx.OperationGTXValue
import net.postchain.gtx.merkle.GtxBinaryTreeFactory
import net.postchain.gtx.merkle.GtxOperationHeadNode

object GtxBinaryTreeFactoryOperation {

    private val mainFactory = GtxBinaryTreeFactory()

    /**
     * Design: We add the operation name first in the argument array, and just use same algorithm as for array
     *
     * There are 2 edge cases here:
     * - When there is no operation name. -> Bang out! Not allowed
     * - When there are no arguments. -> We set the right element as empty
     */
    fun buildFromOperationGTXValue(operationGTXValue: OperationGTXValue, gtxPaths: GTXPathSet): BinaryTreeElement {
        val isThisAProofLeaf = gtxPaths.isThisAProofLeaf() // Will tell us if any of the paths points to this element
        //println("Arr,(is proof? $isThisAProofLeaf) Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))

        // 1. Build leaf layer
        val leafList: MutableList<GTXValue> = mutableListOf(operationGTXValue.name)
        for (arg: GTXValue in operationGTXValue.args.asArray()) {
            leafList.add(arg)
        }

        val leafArray = mainFactory.buildLeafElements(leafList, gtxPaths) // Regarding proof paths, we treat the operation as any other array

        // 2. Build all higher layers
        val result = mainFactory.buildHigherLayer(1, leafArray)

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

}