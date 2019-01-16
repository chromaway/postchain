package net.postchain.base.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.gtx.*


/**
 * Builds proofs of the type [GtxMerkleProofTree]
 *
 * Can build from:
 * 1. A binary tree
 * 2. The serialized format
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
    fun buildFromBinaryTree(orginalTree: GtxBinaryTree): GtxMerkleProofTree {

        val rootElement = buildFromBinaryTreeSub(orginalTree.root, calculator)
        return GtxMerkleProofTree(rootElement)
    }

    override fun buildFromBinaryTreeSub(currentElement: BinaryTreeElement,
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

    override fun buildNodeOfCorrectType(currentNode: Node, left: MerkleProofElement, right: MerkleProofElement): ProofNode {
        return when (currentNode) {
            is GtxArrayHeadNode -> ProofNodeGtxArrayHead(currentNode.size, left, right)
            is GtxDictHeadNode -> ProofNodeGtxDictHead(currentNode.size, left, right)
            is Node -> ProofNodeSimple(left, right)
            else -> throw IllegalStateException("Should have taken care of this node type: $currentNode")
        }
    }

    // ---------- Deserialization -----
    /**
     * Builds the [GtxMerkleProofTree] from a serialized [ArrayGTXValue] object graph.
     *
     * @param serializedRootArrayGtx is the root element of the serialized structure
     * @return the proof tree as it looked before serialization.
     */
    fun deserialize(serializedRootArrayGtx: ArrayGTXValue): GtxMerkleProofTree {

        val rootElement = deserializeSub(serializedRootArrayGtx)
        return GtxMerkleProofTree(rootElement)
    }


    override fun deserializeSubOther(typeCode: Long, currentSerializedArrayGtx: ArrayGTXValue): MerkleProofElement {

        return when (typeCode) {
            SERIALIZATION_ARRAY_TYPE  ->  {
                val size = (currentSerializedArrayGtx.get(1) as IntegerGTXValue).integer as Int
                val left: MerkleProofElement = deserializeSub(currentSerializedArrayGtx.get(2) as ArrayGTXValue)
                val right: MerkleProofElement = deserializeSub(currentSerializedArrayGtx.get(3) as ArrayGTXValue)
                ProofNodeGtxArrayHead(size, left, right)
            }
            SERIALIZATION_DICT_TYPE  ->  {
                val size = (currentSerializedArrayGtx.get(1) as IntegerGTXValue).integer as Int
                val left: MerkleProofElement = deserializeSub(currentSerializedArrayGtx.get(2) as ArrayGTXValue)
                val right: MerkleProofElement = deserializeSub(currentSerializedArrayGtx.get(3) as ArrayGTXValue)
                ProofNodeGtxDictHead(size, left, right)
            }
            else -> throw IllegalStateException("Should handle the type $typeCode")
        }
    }
}