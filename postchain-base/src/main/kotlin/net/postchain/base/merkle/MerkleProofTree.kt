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

data class ProofNode(val left: MerkleProofElement, val right: MerkleProofElement): MerkleProofElement()

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

    fun proveGtxValue(leafToProve: GTXValue): Boolean {
        return false // TODO: fix
    }
}

/**
 * Builds proofs
 */
object MerkleProofTreeFactory {

    /**
     * @param valuesToProveList Note that these instances must BE EXACTLY THE SAME as the one you are looking for.
     *                          (Reason being that there might be many [GTXValue] with the same value and we need
     *                          the position to be correct)
     * @param orginalTree is the tree we will use
     * @param calculator is the class we use for hash calculation
     */
    fun buildMerkleProofTree(valuesToProveList: List<GTXValue>,
                             orginalTree: ContentLeafFullBinaryTree,
                             calculator: MerkleHashCalculator): MerkleProofTree {

        val rootElement = buildSubProofTree(valuesToProveList, orginalTree.root, calculator)
        return MerkleProofTree(rootElement)
    }

    fun buildSubProofTree(valuesToProveList: List<GTXValue>,
                          currentElement: FbtElement,
                          calculator: MerkleHashCalculator): MerkleProofElement {
        return when (currentElement) {
            is Leaf -> {
                var foundGTXValue: GTXValue? = null
                for (valueToProve: GTXValue in valuesToProveList) {
                    if (currentElement.content === valueToProve) {
                        // Note that we compared using referential (===) identity!
                        // Needed here since there might be many GTXValues with the same content, and they are NOT
                        // the same since they are in different parts of the tree.
                        foundGTXValue = valueToProve
                        break
                    }
                }
                if (foundGTXValue == null) {
                    ProofHashedLeaf(calculator.calculateLeafHash(currentElement.content))
                } else {
                    ProofGtxLeaf(currentElement.content)
                }
            }
            is Node -> {
                val left = buildSubProofTree(valuesToProveList, currentElement.left, calculator)
                val right = buildSubProofTree(valuesToProveList, currentElement.right, calculator)
                if (left is ProofHashedLeaf && right is ProofHashedLeaf) {
                    // If both children are hashes, then
                    // we must reduce them to a new (combined) hash.
                    val addedHash = calculator.calculateNodeHash(left.hash, right.hash)
                    ProofHashedLeaf(addedHash)
                } else {
                    ProofNode(left, right)
                }
            }
        }

    }



}
