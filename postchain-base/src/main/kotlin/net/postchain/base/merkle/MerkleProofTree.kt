package net.postchain.base.merkle

import net.postchain.gtx.GTXValue


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

//enum class Side { LEFT, RIGHT }

typealias Hash = ByteArray

sealed class MerkleProofElement

/**
 * Just a node. Usually one of the sides (left or right) holds a [ProofHashedLeaf] and the other side
 * holds a [ProofGtxLeaf] or [ProofNode].
 */
data class ProofNode(val prefix: Byte, val left: MerkleProofElement, val right: MerkleProofElement): MerkleProofElement()

/**
 * The data we want to prove exists in the Merkle Tree
 * Note: We allow for many [ProofGtxLeaf] in the same proof
 * (although proofs of one value will probably be the most common case)
  */
data class ProofGtxLeaf(val content: GTXValue): MerkleProofElement()

/**
 * The hash in this leaf is a hash of an entire sub tree of the original Merkle tree
 */
data class ProofHashedLeaf(val hash: Hash): MerkleProofElement()

/**
 * The "Proof Tree" can be used to prove that one or more [GTXValue] is/are indeed part of the Merkle tree
 * We will use the [MerkleProofTree] to calculate the BlockRID (see doc in top of this file)
 */
class MerkleProofTree(val root: MerkleProofElement) {

    /**
     * @return the calculated merkle root of the proof. For the proof to be valid, this [Hash] should equal the
     *          merkle root of the block.
     */
    fun calculateMerkleRoot(calculator: MerkleHashCalculator): Hash {
        return calculateMerkleRootInternal(root, calculator)
    }

    private fun calculateMerkleRootInternal(currentElement: MerkleProofElement, calculator: MerkleHashCalculator): Hash {
        return when (currentElement) {
            is ProofHashedLeaf -> currentElement.hash
            is ProofGtxLeaf -> calculator.calculateLeafHash(currentElement.content)
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
            is ProofGtxLeaf -> 1
            is ProofHashedLeaf -> 1
            is ProofNode -> maxOf(maxLevelInternal(node.left), maxLevelInternal(node.right)) + 1
        }
    }

}

/**
 * Builds proofs
 */
object MerkleProofTreeFactory {

    /**
     * Builds the [MerkleProofTree] from the [GtxBinaryTree]
     * Note that the [GtxBinaryTree] already has marked all elements that should be proven, so all we have to
     * do now is to convert the rest to hashes.
     *
     * @param orginalTree is the tree we will use
     * @param calculator is the class we use for hash calculation
     */
    fun buildMerkleProofTree(orginalTree: GtxBinaryTree,
                             calculator: MerkleHashCalculator): MerkleProofTree {

        val rootElement = buildSubProofTree(orginalTree.root, calculator)
        return MerkleProofTree(rootElement)
    }

    private fun buildSubProofTree(currentElement: BinaryTreeElement,
                                  calculator: MerkleHashCalculator): MerkleProofElement {
        return when (currentElement) {
            is EmptyLeaf -> {
                ProofHashedLeaf(ByteArray(32)) // Just zeros
            }
            is Leaf<*> -> {
                if (currentElement.isPathLeaf()) {
                    // Don't convert it
                    println("Prove the leaf with content: " + currentElement.content)
                    ProofGtxLeaf(currentElement.content as GTXValue)
                } else {
                    // Make it a hash
                    println("Hash the leaf with content: " + currentElement.content)
                    ProofHashedLeaf(calculator.calculateLeafHash(currentElement.content as GTXValue))
                }
            }
            is SubTreeRootNode<*> ->  {
                if (currentElement.isPathLeaf()) {
                    // Don't convert it
                    println("Prove the node with content: " + currentElement.content)
                    ProofGtxLeaf(currentElement.content as GTXValue)
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

    private fun convertNode(currentNode: Node, calculator: MerkleHashCalculator): MerkleProofElement {
        val left = buildSubProofTree(currentNode.left, calculator)
        val right = buildSubProofTree(currentNode.right, calculator)
        return if (left is ProofHashedLeaf && right is ProofHashedLeaf) {
            // If both children are hashes, then
            // we must reduce them to a new (combined) hash.
            val addedHash = calculator.calculateNodeHash(currentNode.getPrefixByte(), left.hash, right.hash)
            ProofHashedLeaf(addedHash)
        } else {
            ProofNode(currentNode.getPrefixByte(), left, right)
        }
    }

}
