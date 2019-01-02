package net.postchain.base.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXValue

/**
 * See [MerkleProofTree] for documentation
 */
class GtxMerkleProofTree(root: MerkleProofElement): MerkleProofTree<GTXValue, GTXPath>(root) {
}

/**
 * Builds proofs
 */
class GtxMerkleProofTreeFactory(calculator: MerkleHashCalculator<GTXValue, GTXPath>): MerkleProofTreeFactory<GTXValue, GTXPath>(calculator)  {

    /**
     * Builds the [MerkleProofTree] from the [GtxBinaryTree]
     * Note that the [GtxBinaryTree] already has marked all elements that should be proven, so all we have to
     * do now is to convert the rest to hashes.
     *
     * @param orginalTree is the tree we will use
     * @param calculator is the class we use for hash calculation
     */
    fun buildGtxMerkleProofTree(orginalTree: GtxBinaryTree): GtxMerkleProofTree {

        val rootElement = buildSubProofTree(orginalTree.root, calculator)
        return GtxMerkleProofTree(rootElement)
    }

    override fun buildSubProofTree(currentElement: BinaryTreeElement,
                                  calculator: MerkleHashCalculator<GTXValue, GTXPath>): MerkleProofElement {
        return when (currentElement) {
            is EmptyLeaf -> {
                ProofHashedLeaf(MerkleBasics.EMPTY_HASH) // Just zeros
            }
            is Leaf<*> -> {
                val content: GTXValue = currentElement.content as GTXValue // Have to convert here

                if (currentElement.isPathLeaf()) {
                    // Don't convert it
                    println("Prove the leaf with content: $content")
                    ProofValueLeaf(content)
                } else {
                    // Make it a hash
                    println("Hash the leaf with content: $content")
                    ProofHashedLeaf(calculator.calculateLeafHash(content))
                }
            }
            is SubTreeRootNode<*> ->  {
                val content: GTXValue = currentElement.content as GTXValue

                if (currentElement.isPathLeaf()) {
                    // Don't convert it
                    println("Prove the node with content: $content")
                    ProofValueLeaf(content)
                } else {
                    // Convert
                    println("Convert node ")
                    convertNode(currentElement, calculator)
                }
            }
            is Node -> {
                convertNode(currentElement, calculator)
            }
            else -> throw IllegalStateException("Cannot handle $currentElement")
        }
    }


}