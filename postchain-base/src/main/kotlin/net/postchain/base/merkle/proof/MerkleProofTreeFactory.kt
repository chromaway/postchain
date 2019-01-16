package net.postchain.base.merkle.proof

import net.postchain.base.merkle.BinaryTree
import net.postchain.base.merkle.BinaryTreeElement
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.base.merkle.Node
import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.ByteArrayGTXValue
import net.postchain.gtx.IntegerGTXValue


/**
 * Base class for building [MerkleProofTree] (but needs to be overridden to actually do something)
 *
 * Can build from:
 * 1. A binary tree (this means you first have to build a binary tree before you can build the proof,
 *     see [BinaryTreeFactory] )
 * 2. The serialized format
 */
abstract class MerkleProofTreeFactory<T,TPath>(val calculator: MerkleHashCalculator<T, TPath>) {

    /**
     * Converts [BinaryTreeElement] into a [MerkleProofElement].
     *
     * Note: that the [BinaryTree] already has marked all elements that should be proven, so all we have to
     * do now is to convert the rest to hashes.
     *
     * @param currentElement is the element we will use as root of the tree
     * @param calculator is the class we use for hash calculation
     * @return the [MerkleProofElement] we have built.
     */
    abstract fun buildFromBinaryTreeSub(currentElement: BinaryTreeElement, calculator: MerkleHashCalculator<T, TPath>):
            MerkleProofElement


    protected fun convertNode(currentNode: Node, calculator: MerkleHashCalculator<T, TPath>): MerkleProofElement {
        val left = buildFromBinaryTreeSub(currentNode.left, calculator)
        val right = buildFromBinaryTreeSub(currentNode.right, calculator)
        return if (left is ProofHashedLeaf && right is ProofHashedLeaf) {
            // If both children are hashes, then
            // we must reduce them to a new (combined) hash.
            val addedHash = calculator.calculateNodeHash(
                    currentNode.getPrefixByte(),
                    left.hash,
                    right.hash)
            ProofHashedLeaf(addedHash)
        } else {
            buildNodeOfCorrectType(currentNode, left, right)

        }
    }

    /**
     * Override this in subclass if there is more than one node type.
     *
     * @param currentNode is the node we should convert to [ProofNode]
     * @return the [ProofNode] implementation that should be used for this [Node]
     */
    open fun buildNodeOfCorrectType(currentNode: Node, left: MerkleProofElement, right: MerkleProofElement): ProofNode {
        return ProofNode(currentNode.getPrefixByte(), left, right)
    }


    // ---------- Deserialization -----
    /**
     * Call this method from sub class to deserialize
     */
    protected fun deserializeSub(currentSerializedArrayGtx: ArrayGTXValue): MerkleProofElement {

        val head = currentSerializedArrayGtx.get(0)
        val typeCode = (head as IntegerGTXValue).integer
        val secondElement = currentSerializedArrayGtx.get(1)
        return when (typeCode) {
            SERIALIZATION_HASH_LEAF_TYPE -> {
                val byteArray = secondElement as ByteArrayGTXValue
                ProofHashedLeaf(byteArray.bytearray)
            }
            SERIALIZATION_VALUE_LEAF_TYPE -> ProofValueLeaf(secondElement)
            SERIALIZATION_NODE_TYPE -> {
                val left: MerkleProofElement = deserializeSub(secondElement as ArrayGTXValue)
                val right: MerkleProofElement = deserializeSub(currentSerializedArrayGtx.get(2) as ArrayGTXValue)
                ProofNodeSimple(left, right)
            }
            else -> deserializeSubOther(typeCode, currentSerializedArrayGtx)
        }
    }

    /**
     * Override this to handle extra types (defined in subclass)
     */
    open fun deserializeSubOther(typeCode: Long, currentSerializedArrayGtx: ArrayGTXValue): MerkleProofElement {
        throw IllegalStateException("Override this method to handle type: $typeCode")
    }



}