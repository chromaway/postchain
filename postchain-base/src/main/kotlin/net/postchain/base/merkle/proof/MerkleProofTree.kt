package net.postchain.base.merkle.proof

import net.postchain.base.merkle.*

/**
 * (Here is some info about Merkle Proofs and the way we tackle it)
 * This code works with a binary tree we call [MerkleProofTree].
 * The [MerkleProofTree] is a clone of the original Merkle tree, but where most values are replaced by hashes.
 * The purpose of the [MerkleProofTree] is that it can be used to prove that one of the values NOT replaced by
 *   a hash (for example V1) is part of the original Merkle tree.
 *
 *  -------------
 * Example1:
 *
 *                root
 *         node1       hash2
 *      hash1  V1
 *  -------------
 *
 *  If "x" is the root hash of the original Merkle tree, than the proof of V1 (in Example1 above) is:
 *
 *  -------------
 *  x == hash(
 *          hash( hash1 + hash(V1)))
 *          +
 *          hash2
 *       )
 *  -------------
 *
 *  What makes the calculation a bit more complicated is that we also allow [MerkleProofTree] to hold
 *  multiple values (V1 and V2 in Example2):
 *  -------------
 *  Example2:
 *
 *                root
 *         node1       node2
 *      hash1  V1    hash2   V2
 *  -------------
 *
 *  In this case the proof of V1 and V2 will look like this:
 *  -------------
 *  x == hash(
 *          hash( hash1 + hash(V1)))
 *          +
 *          hash( hash2 + hash(V2)))
 *       )
 *  -------------
 *
 */

sealed class MerkleProofElement

/**
 * Just a node. Usually one of the sides (left or right) holds a [ProofHashedLeaf] and the other side
 * holds a [ProofValueLeaf] or [ProofNode].
 */
data class ProofNode(val prefix: Byte, val left: MerkleProofElement, val right: MerkleProofElement): MerkleProofElement()

/**
 * The data we want to prove exists in the Merkle Tree
 * Note: We allow for many [ProofValueLeaf] in the same proof
 * (although proofs of one value will probably be the most common case)
 */
data class ProofValueLeaf<T>(val content: T): MerkleProofElement()

/**
 * The hash in this leaf is a hash of an entire sub tree of the original Merkle tree
 */
data class ProofHashedLeaf<T>(val hash: T): MerkleProofElement()

/**
 * The "Proof Tree" can be used to prove that one or more values is/are indeed part of the Merkle tree
 * We will use the [MerkleProofTree] to calculate the BlockRID (see doc in top of this file)
 */
open class MerkleProofTree<T>(val root: MerkleProofElement) {

    /**
     * @return the calculated merkle root of the proof. For the proof to be valid, this [Hash] should equal the
     *          merkle root of the block.
     */
    fun calculateMerkleRoot(calculator: MerkleHashCalculator<T>): Hash {
        return calculateMerkleRootInternal(root, calculator)
    }

    private fun calculateMerkleRootInternal(currentElement: MerkleProofElement, calculator: MerkleHashCalculator<T>): Hash {
        return when (currentElement) {
            is ProofHashedLeaf<*> -> currentElement.hash as Hash
            is ProofValueLeaf<*> -> calculator.calculateLeafHash(currentElement.content as T)
            is ProofNode -> {
                val left = calculateMerkleRootInternal(currentElement.left, calculator)
                val right = calculateMerkleRootInternal(currentElement.right, calculator)
                calculator.calculateNodeHash(currentElement.prefix ,left, right)
            }
        }
    }

    /**
     * Mostly for debugging
     */
    fun maxLevel(): Int {
        return maxLevelInternal(root)
    }

    private fun maxLevelInternal(node: MerkleProofElement): Int {
        return when (node) {
            is ProofValueLeaf<*> -> 1
            is ProofHashedLeaf<*> -> 1
            is ProofNode -> maxOf(maxLevelInternal(node.left), maxLevelInternal(node.right)) + 1
        }
    }

}

/**
 * Builds [MerkleProofTree] (but needs to be overridden
 */
abstract class MerkleProofTreeFactory<T>(val calculator: MerkleHashCalculator<T>) {

    /**
     * Builds the [MerkleProofTree] from the [BinaryTree]
     * Note that the [BinaryTree] already has marked all elements that should be proven, so all we have to
     * do now is to convert the rest to hashes.
     *
     * @param orginalTree is the tree we will use
     * @param calculator is the class we use for hash calculation
     */
    fun buildMerkleProofTree(orginalTree: BinaryTree<T>): MerkleProofTree<T> {

        val rootElement = buildSubProofTree(orginalTree.root, calculator)
        return MerkleProofTree(rootElement)
    }


    abstract fun buildSubProofTree(currentElement: BinaryTreeElement,
                               calculator: MerkleHashCalculator<T>): MerkleProofElement


    protected fun convertNode(currentNode: Node, calculator: MerkleHashCalculator<T>): MerkleProofElement {
        val left = buildSubProofTree(currentNode.left, calculator)
        val right = buildSubProofTree(currentNode.right, calculator)
        return if (left is ProofHashedLeaf<*> && right is ProofHashedLeaf<*>) {
            // If both children are hashes, then
            // we must reduce them to a new (combined) hash.
            val addedHash = calculator.calculateNodeHash(
                    currentNode.getPrefixByte(),
                    left.hash as Hash,
                    right.hash as Hash)
            ProofHashedLeaf(addedHash)
        } else {
            ProofNode(currentNode.getPrefixByte(), left, right)
        }
    }
}
