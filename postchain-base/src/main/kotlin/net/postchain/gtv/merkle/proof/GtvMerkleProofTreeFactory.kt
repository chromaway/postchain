package net.postchain.gtv.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.base.merkle.proof.*
import net.postchain.gtv.*
import net.postchain.gtv.merkle.GtvArrayHeadNode
import net.postchain.gtv.merkle.GtvBinaryTree
import net.postchain.gtv.merkle.GtvDictHeadNode


/**
 * Builds proofs of the type [GtvMerkleProofTree]
 *
 * Can build from:
 * 1. A binary tree
 * 2. The serialized format
 */
class GtvMerkleProofTreeFactory(calculator: MerkleHashCalculator<Gtv>): MerkleProofTreeFactory<Gtv>(calculator)  {

    /**
     * Builds the [MerkleProofTree] from the [GtvBinaryTree]
     * Note that the [GtvBinaryTree] already has marked all elements that should be proven, so all we have to
     * do now is to convert the rest to hashes.
     *
     * @param orginalTree is the tree we will use
     * @param calculator is the class we use for hash calculation
     */
    fun buildFromBinaryTree(orginalTree: GtvBinaryTree): GtvMerkleProofTree {

        val rootElement = buildFromBinaryTreeSub(orginalTree.root, calculator)
        return GtvMerkleProofTree(rootElement)
    }

    override fun buildFromBinaryTreeSub(currentElement: BinaryTreeElement,
                               calculator: MerkleHashCalculator<Gtv>): MerkleProofElement {
        return when (currentElement) {
            is EmptyLeaf -> {
                ProofHashedLeaf(MerkleBasics.EMPTY_HASH) // Just zeros
            }
            is Leaf<*> -> {
                val content: Gtv = currentElement.content as Gtv // Have to convert here

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
                val content: Gtv = currentElement.content as Gtv

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

    override fun buildNodeOfCorrectType(currentNode: Node, left: MerkleProofElement, right: MerkleProofElement): ProofNode {
        return when (currentNode) {
            is GtvArrayHeadNode -> ProofNodeGtvArrayHead(currentNode.size, left, right)
            is GtvDictHeadNode -> ProofNodeGtvDictHead(currentNode.size, left, right)
            is Node -> ProofNodeSimple(left, right)
            else -> throw IllegalStateException("Should have taken care of this node type: $currentNode")
        }
    }

    // ---------- Deserialization -----
    /**
     * Builds the [GtvMerkleProofTree] from a serialized [GtvArray] object graph.
     *
     * @param serializedRootArrayGtv is the root element of the serialized structure
     * @return the proof tree as it looked before serialization.
     */
    fun deserialize(serializedRootArrayGtv: GtvArray): GtvMerkleProofTree {

        val rootElement = deserializeSub(serializedRootArrayGtv)
        return GtvMerkleProofTree(rootElement)
    }

    fun deserializeSub(currentSerializedArrayGtv: GtvArray): MerkleProofElement {

        val head = currentSerializedArrayGtv.get(0)
        val typeCode = (head as GtvInteger).integer
        val secondElement = currentSerializedArrayGtv.get(1)
        return when (typeCode) {
            SERIALIZATION_HASH_LEAF_TYPE -> {
                val byteArray = secondElement as GtvByteArray
                ProofHashedLeaf(byteArray.bytearray)
            }
            SERIALIZATION_VALUE_LEAF_TYPE -> ProofValueLeaf(secondElement)
            SERIALIZATION_NODE_TYPE -> {
                val left: MerkleProofElement = deserializeSub(secondElement as GtvArray)
                val right: MerkleProofElement = deserializeSub(currentSerializedArrayGtv.get(2) as GtvArray)
                ProofNodeSimple(left, right)
            }
            SERIALIZATION_ARRAY_TYPE ->  {
                val size = (currentSerializedArrayGtv.get(1) as GtvInteger).integer.toInt()
                val left: MerkleProofElement = deserializeSub(currentSerializedArrayGtv.get(2) as GtvArray)
                val right: MerkleProofElement = deserializeSub(currentSerializedArrayGtv.get(3) as GtvArray)
                ProofNodeGtvArrayHead(size, left, right)
            }
            SERIALIZATION_DICT_TYPE ->  {
                val size = (currentSerializedArrayGtv.get(1) as GtvInteger).integer.toInt()
                val left: MerkleProofElement = deserializeSub(currentSerializedArrayGtv.get(2) as GtvArray)
                val right: MerkleProofElement = deserializeSub(currentSerializedArrayGtv.get(3) as GtvArray)
                ProofNodeGtvDictHead(size, left, right)
            }
            else -> throw IllegalStateException("Should handle the type $typeCode")
        }
    }
}