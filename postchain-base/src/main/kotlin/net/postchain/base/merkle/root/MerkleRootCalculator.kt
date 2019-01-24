package net.postchain.base.merkle.root

import net.postchain.base.CryptoSystem
import net.postchain.base.merkle.*
import net.postchain.base.merkle.MerkleBasics.EMPTY_HASH
import net.postchain.gtx.merkle.GtxMerkleHashCalculator

object MerkleRootCalculator {

    val hashTreeFactory = HashBinaryTreeFactory()

    /**
     * Computes merkle root hash from a list of hashes
     *
     * @param cryptoSystem - needed to find the hash/digest function
     * @param hashes - all the hashes we will use as leaves in the tree
     *  @return the merkle root
     */
    fun computeMerkleRoot(cryptoSystem: CryptoSystem,
                              hashes: Array<Hash>): Hash {
        val treeWithHashesAsLeaves = hashTreeFactory.buildFromList(hashes.toList())
        return computeMerkleRoot(treeWithHashesAsLeaves, GtxMerkleHashCalculator(cryptoSystem))
    }

    /**
     * Computes merkle root hash from a tree of hashes
     *
     * @param hashBinaryTree
     * @param calculator
     */
    fun computeMerkleRoot(hashBinaryTree: HashBinaryTree, calculator: BinaryNodeHashCalculator): Hash {
        val element: BinaryTreeElement = hashBinaryTree.root
        return computeSubTreeHash(element, calculator)
    }

    /**
     * Inner recursion
     */
    private fun computeSubTreeHash(element: BinaryTreeElement, calculator: BinaryNodeHashCalculator): Hash {
        return when(element) {
            is EmptyLeaf -> {
                return EMPTY_HASH
            }
            is Leaf<*> -> {
                val x = element.content
                x as Hash // No need to calculate the leaf hash. That's what we started out with
            }
            is Node -> {
                val left = computeSubTreeHash(element.left, calculator)
                val right = computeSubTreeHash(element.right, calculator)

                calculator.calculateNodeHash(element.getPrefixByte(), left, right)
            }
            else -> {
                throw IllegalStateException("TODO fix") // TODO fix
            }
        }
    }
}