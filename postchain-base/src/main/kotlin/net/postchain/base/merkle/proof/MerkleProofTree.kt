// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.merkle.proof

import net.postchain.common.data.Hash
import net.postchain.base.merkle.MerkleBasics.HASH_PREFIX_NODE
import net.postchain.base.merkle.MerkleBasics.UNKNOWN_SIZE_IN_BYTE
import java.util.*

const val SERIALIZATION_HASH_LEAF_TYPE: Long = 100
const val SERIALIZATION_VALUE_LEAF_TYPE: Long = 101
const val SERIALIZATION_NODE_TYPE: Long = 102

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

interface MerkleProofElement

/**
 * Base class for nodes
 *
 * @property prefix is the prefix to add during merkle root hash calculation
 * @property left is the left sub tree
 * @property right is the right sub tree
 */
open class ProofNode(val prefix: Byte, val left: MerkleProofElement, val right: MerkleProofElement): MerkleProofElement

/**
 * A dummy node that does not represent anything in the original structure.
 * Usually one of the sides (left or right) holds a [ProofHashedLeaf] and the other side holds a [ProofValueLeaf] or [ProofNode].
 *
 * @property left (see super)
 * @property right (see super)
 */
class ProofNodeSimple(left: MerkleProofElement, right: MerkleProofElement): ProofNode(HASH_PREFIX_NODE, left, right)

/**
 * The data we want to prove exists in the Merkle Tree
 * Note: We allow for many [ProofValueLeaf] in the same proof
 * (although proofs of one value will probably be the most common case)
 *
 * @property content is the value to be proven (in its raw form)
 * @property sizeInBytes is the nr of bytes the original object takes up
 * @property pathElem is the path element that tells us how to find this element in the surrounding collection
 */
open class ProofValueLeaf<T>(val content: T, val sizeInBytes: Int): MerkleProofElement

/**
 * The hash in this leaf is a hash of an entire sub tree of the original Merkle tree
 *
 * @property merkleHash is the hash of the sub tree that isn't interesting for this proof
 */
data class ProofHashedLeaf(val merkleHash: Hash): MerkleProofElement {

    // (Probably not needed but I implement equals to get rid of the warning)
    override fun equals(other: Any?): Boolean {
        return if (other is ProofHashedLeaf) {
            Arrays.equals(this.merkleHash, other.merkleHash)
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(merkleHash)
    }
}



/**
 * The "Proof Tree" can be used to prove that one or more values is/are indeed part of the Merkle tree
 * We will use the [MerkleProofTree] to calculate the BlockRID (see doc in top of this file)
 */
abstract class MerkleProofTree<T>(val root: MerkleProofElement, val totalNrOfBytes: Int = UNKNOWN_SIZE_IN_BYTE ) {


    /**
     * Mostly for debugging
     */
    fun maxLevel(): Int {
        return maxLevelInternal(root)
    }

    private fun maxLevelInternal(node: MerkleProofElement): Int {
        return when (node) {
            is ProofValueLeaf<*> -> 1
            is ProofHashedLeaf -> 1
            is ProofNode -> maxOf(maxLevelInternal(node.left), maxLevelInternal(node.right)) + 1
            else -> throw IllegalStateException("Should be able to handle node type: $node")
        }
    }

}

