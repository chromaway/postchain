package net.postchain.base.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.base.merkle.MerkleBasics.HASH_PREFIX_NODE
import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.ByteArrayGTXValue
import net.postchain.gtx.GTXValue
import net.postchain.gtx.IntegerGTXValue

const val SERIALIZATION_HASH_LEAF_TYPE: Long = 0
const val SERIALIZATION_VALUE_LEAF_TYPE: Long = 1
const val SERIALIZATION_NODE_TYPE: Long = 2

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

open class MerkleProofElement

/**
 * Base class for nodes
 */
open class ProofNode(val prefix: Byte, val left: MerkleProofElement, val right: MerkleProofElement): MerkleProofElement()

/**
 * A dummy node that does not represent anything in the original structure.
 * Usually one of the sides (left or right) holds a [ProofHashedLeaf] and the other side holds a [ProofValueLeaf] or [ProofNode].
 */
class ProofNodeSimple(left: MerkleProofElement, right: MerkleProofElement): ProofNode(HASH_PREFIX_NODE, left, right)

/**
 * The data we want to prove exists in the Merkle Tree
 * Note: We allow for many [ProofValueLeaf] in the same proof
 * (although proofs of one value will probably be the most common case)
 */
data class ProofValueLeaf<T>(val content: T): MerkleProofElement()

/**
 * The hash in this leaf is a hash of an entire sub tree of the original Merkle tree
 */
data class ProofHashedLeaf(val hash: Hash): MerkleProofElement() {

    // (Probably not needed but I implement equals to get rid of the warning)
    override fun equals(other: Any?): Boolean {
        return if (other is ProofHashedLeaf) {
            this.hash.contentEquals(other.hash)
        } else {
            false
        }
    }
}

/**
 * The "Proof Tree" can be used to prove that one or more values is/are indeed part of the Merkle tree
 * We will use the [MerkleProofTree] to calculate the BlockRID (see doc in top of this file)
 */
abstract class MerkleProofTree<T,TPath>(val root: MerkleProofElement) {



    /**
     * Note: When calculating the merkle root of a proof of a complicated structure (array or dict)
     *       means that the value-to-be-proved (i.e. array/dict) must be transformed to a binary tree
     *       before we can calculate it's hash.
     *
     * @return the calculated merkle root of the proof. For the proof to be valid, this [Hash] should equal the
     *          merkle root of the block.
     */
    fun calculateMerkleRoot(calculator: MerkleHashCalculator<T,TPath>): Hash {
        return calculateMerkleRootInternal(root, calculator)
    }

    private fun calculateMerkleRootInternal(currentElement: MerkleProofElement, calculator: MerkleHashCalculator<T,TPath>): Hash {
        return when (currentElement) {
            is ProofHashedLeaf -> currentElement.hash
            is ProofValueLeaf<*> -> {
                val value = currentElement.content as T
                if (calculator.isContainerProofValueLeaf(value)) {
                    // We have a container value to prove, so need to convert the value to a binary tree, and THEN hash it
                    val merkleProofTree: MerkleProofTree<T,TPath> = calculator.buildTreeFromContainerValue(value)
                    calculateMerkleRootInternal(merkleProofTree.root, calculator)
                } else {
                    // This is a primitive value, just hash it
                    calculator.calculateLeafHash(value)
                }
            }
            is ProofNode -> {
                val left = calculateMerkleRootInternal(currentElement.left, calculator)
                val right = calculateMerkleRootInternal(currentElement.right, calculator)
                calculator.calculateNodeHash(currentElement.prefix ,left, right)
            }
            else -> {
                throw IllegalStateException("Should have handled this type: $currentElement")
            }
        }
    }


    /**
     * Note that we have our own primitive serialization format. It is based on arrays that begins with an integer.
     * The integer tells us what the content is.
     * 0 -> just a hash
     * 1 -> value to be proven
     * 2 -> a node
     * (we can add more in sub classes)
     */
    fun serializeToGtx(): ArrayGTXValue {

        return serializeToGtxInternal(this.root)
    }

    protected fun serializeToGtxInternal(currentElement: MerkleProofElement): ArrayGTXValue {
        return when (currentElement) {
            is ProofHashedLeaf -> {
                val tail = ByteArrayGTXValue(currentElement.hash)
                val head = IntegerGTXValue(SERIALIZATION_HASH_LEAF_TYPE)
                val arr: Array<GTXValue> = arrayOf(head, tail)
                ArrayGTXValue(arr)
            }
            is ProofValueLeaf<*> -> {
                val tail = serializeValueLeafToGtx(currentElement.content as T)
                val head = IntegerGTXValue(SERIALIZATION_VALUE_LEAF_TYPE)
                val arr: Array<GTXValue> = arrayOf(head, tail)
                ArrayGTXValue(arr)
            }
            is ProofNodeSimple -> {
                val tail1 = serializeToGtxInternal(currentElement.left)
                val tail2 = serializeToGtxInternal(currentElement.right)
                val head = IntegerGTXValue(SERIALIZATION_NODE_TYPE)
                val arr: Array<GTXValue> = arrayOf(head, tail1, tail2)
                ArrayGTXValue(arr)
            }
            else -> serializeOtherTypes(currentElement)
        }
    }

    abstract fun serializeValueLeafToGtx(valueLeaf: T): GTXValue


    abstract fun serializeOtherTypes(currentElement: MerkleProofElement): ArrayGTXValue


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

