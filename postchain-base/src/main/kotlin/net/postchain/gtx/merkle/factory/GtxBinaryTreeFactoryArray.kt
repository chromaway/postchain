package net.postchain.gtx.merkle.factory

import net.postchain.base.merkle.BinaryTreeElement
import net.postchain.base.merkle.EmptyLeaf
import net.postchain.base.merkle.Leaf
import net.postchain.base.merkle.Node
import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.GTXPathSet
import net.postchain.gtx.GTXValue
import net.postchain.gtx.merkle.GtxArrayHeadNode
import net.postchain.gtx.merkle.GtxBinaryTreeFactory

object GtxBinaryTreeFactoryArray {

    private val mainFactory = GtxBinaryTreeFactory()

    /**
     * There are 2 edge cases here:
     * - When the args is empty. -> We return a top node with two empty leafs
     * - When there is only one element. -> We set the right element as empty
     */
    fun buildFromArrayGTXValue(arrayGTXValue: ArrayGTXValue, gtxPaths: GTXPathSet): BinaryTreeElement {
        val isThisAProofLeaf = gtxPaths.isThisAProofLeaf() // Will tell us if any of the paths points to this element
        //println("Arr,(is proof? $isThisAProofLeaf) Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))

        // 1. Build leaf layer
        val leafList: List<GTXValue> = arrayGTXValue.array.map {it}
        if (leafList.isEmpty()) {
            return GtxArrayHeadNode(EmptyLeaf, EmptyLeaf, isThisAProofLeaf, arrayGTXValue, 0)
        }

        val leafArray = mainFactory.buildLeafElements(leafList, gtxPaths)

        // 2. Build all higher layers
        val result = mainFactory.buildHigherLayer(1, leafArray)

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

}