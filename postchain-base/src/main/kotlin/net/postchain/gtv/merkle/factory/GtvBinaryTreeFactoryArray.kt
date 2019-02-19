package net.postchain.gtv.merkle.factory

import net.postchain.base.merkle.*
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvPathSet
import net.postchain.gtv.Gtv
import net.postchain.gtv.merkle.GtvArrayHeadNode
import net.postchain.gtv.merkle.GtvBinaryTreeFactory

object GtvBinaryTreeFactoryArray {

    private val mainFactory = GtvBinaryTreeFactory()

    /**
     * There are 2 edge cases here:
     * - When the args is empty. -> We return a top node with two empty leafs
     * - When there is only one element. -> We set the right element as empty
     */
    fun buildFromGtvArray(GtvArray: GtvArray, GtvPaths: GtvPathSet, memoization: MerkleHashMemoization<Gtv>): BinaryTreeElement {
        val isThisAProofLeaf = GtvPaths.isThisAProofLeaf() // Will tell us if any of the paths points to this element
        //println("Arr,(is proof? $isThisAProofLeaf) Proof path (size: ${GtvPathList.size} ) list: " + GtvPath.debugRerpresentation(GtvPathList))

        // 1. Build leaf layer
        val leafList: List<Gtv> = GtvArray.array.map {it}
        if (leafList.isEmpty()) {
            return GtvArrayHeadNode(EmptyLeaf, EmptyLeaf, isThisAProofLeaf, GtvArray, 0, 0)
        }

        val leafArray = mainFactory.buildLeafElements(leafList, GtvPaths, memoization)
        val sumNrOfBytes = leafArray.sumBy { it.getNrOfBytes() }

        // 2. Build all higher layers
        val result = mainFactory.buildHigherLayer(1, leafArray)

        // 3. Fix and return the root node
        val orgRoot = result.get(0)
        return when (orgRoot) {
            is Node -> {
                GtvArrayHeadNode(orgRoot.left, orgRoot.right, isThisAProofLeaf, GtvArray, leafList.size, sumNrOfBytes)
            }
            is Leaf<*> -> {
                if (leafList.size > 1) {
                    throw IllegalStateException("How come we got a leaf returned when we had ${leafList.size} elements is the args?")
                } else {
                    // Create a dummy to the right
                    GtvArrayHeadNode(orgRoot, EmptyLeaf, isThisAProofLeaf, GtvArray, leafList.size, sumNrOfBytes)
                }
            }
            else -> throw IllegalStateException("Should not find element of this type here: $orgRoot")
        }
    }

}