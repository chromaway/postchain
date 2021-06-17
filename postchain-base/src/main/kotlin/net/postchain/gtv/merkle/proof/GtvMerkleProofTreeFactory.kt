// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.base.merkle.proof.*
import net.postchain.core.UserMistake
import net.postchain.gtv.*
import net.postchain.gtv.merkle.GtvArrayHeadNode
import net.postchain.gtv.merkle.GtvBinaryTree
import net.postchain.gtv.merkle.GtvDictHeadNode
import net.postchain.gtv.merkle.GtvMerkleBasics
import net.postchain.gtv.path.*
import net.postchain.common.data.Hash



/**
 * Builds proofs of the type [GtvMerkleProofTree]
 *
 * Can build from:
 * 1. A binary tree
 * 2. The serialized format
 *
 */
class GtvMerkleProofTreeFactory: MerkleProofTreeFactory<Gtv>()   {

    /**
     * Builds the [MerkleProofTree] from the [GtvBinaryTree]
     * Note that the [GtvBinaryTree] already has marked all elements that should be proven, so all we have to
     * do now is to convert the rest to hashes.
     *
     * @param orginalTree is the tree we will use
     * @param calculator is the class we use for hash calculation
     */
    fun buildFromBinaryTree(
            orginalTree: GtvBinaryTree,
            calculator: MerkleHashCalculator<Gtv>
    ): GtvMerkleProofTree {
        if (logger.isTraceEnabled) {
            logger.trace("--------------------------------------------")
            logger.trace("--- Converting binary tree to proof tree ---")
            logger.trace("--------------------------------------------")
        }
        val rootElement = buildFromBinaryTreeInternal(orginalTree.root, calculator)
        if (logger.isTraceEnabled) {
            logger.trace("--------------------------------------------")
            logger.trace("--- /Converting binary tree to proof tree --")
            logger.trace("--------------------------------------------")
        }
        return GtvMerkleProofTree(rootElement, orginalTree.root.getNrOfBytes())
    }

    override fun buildFromBinaryTreeInternal(
            currentElement: BinaryTreeElement,
            calculator: MerkleHashCalculator<Gtv>
    ): MerkleProofElement {
        return when (currentElement) {
            is EmptyLeaf -> {
                ProofHashedLeaf(MerkleBasics.EMPTY_HASH) // Just zeros
            }
            is Leaf<*> -> {
                val content: Gtv = currentElement.content as Gtv // Have to convert here

                val pathElem = currentElement.getPathElement()
                if (pathElem != null) {
                    if (pathElem is GtvPathLeafElement) {
                        // Don't convert it
                        if (logger.isTraceEnabled) { logger.trace("Prove the leaf with content: $content") }
                        ProofValueGtvLeaf(content, currentElement.sizeInBytes, pathElem.previous!!)
                    } else {
                        throw UserMistake("The path and structure don't match. We are at a leaf, but path elem is not a leaf: $pathElem ")
                    }
                } else {
                    val hashCarrier: Hash = if (content is GtvNull && content.getCachedMerkleHash() != null) {
                        // Just to take care of the GtvNull case
                        if (logger.isTraceEnabled) { logger.trace("Hash the leaf with GtvNull, no need to calculate since have the hash") }
                        content.getCachedMerkleHash()!!.merkleHash
                    } else {
                        // We don't have paths and we are not in the root element, so we are free to look in cache
                        if (content is GtvPrimitive) {
                            // Not GtvNull -> Make it a hash
                            if (logger.isTraceEnabled) { logger.debug("Hash the leaf with content: $content") }
                            val hashCarrier = calculator.calculateLeafHash(content)
                            hashCarrier
                        } else {
                            logger.warn("What is this leaf that's not a primitive? type: ${content.type}")
                            calculator.calculateLeafHash(content)
                        }
                    }
                    ProofHashedLeaf(hashCarrier)
                }
            }
            is SubTreeRootNode<*> ->  {
                val content: Gtv = currentElement.content as Gtv

                val pathElem = currentElement.getPathElement()
                if (pathElem != null) {
                    if (pathElem is GtvPathLeafElement) {
                        // Don't convert it
                        if (logger.isTraceEnabled) { logger.trace("Prove the node with content: $content") }
                        ProofValueGtvLeaf(content, currentElement.getNrOfBytes(), pathElem.previous!!)
                    } else {
                        if (logger.isTraceEnabled) { logger.trace("Part of a path (=$pathElem), but not leaf so convert") }
                        convertNode(currentElement, calculator)
                    }
                } else {
                    if (logger.isTraceEnabled) { logger.trace("Not part of a path, so convert node of type; ${currentElement::class.simpleName}") }
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
            is GtvArrayHeadNode -> ProofNodeGtvArrayHead(currentNode.size, left, right, extractSearchablePathElement(currentNode))
            is GtvDictHeadNode -> ProofNodeGtvDictHead(currentNode.size, left, right, extractSearchablePathElement(currentNode))
            is Node -> ProofNodeSimple(left, right)
            else -> throw IllegalStateException("Should have taken care of this node type: $currentNode")
        }
    }

    private fun extractSearchablePathElement(currentNode: SubTreeRootNode<Gtv>): SearchableGtvPathElement? {
        val pathElem = currentNode.getPathElement()
        return if (pathElem != null) {
            if (logger.isTraceEnabled) { logger.trace("SubTreeRootNode with pathElem: $pathElem") }
            (pathElem as GtvPathElement).previous
        } else {
            if (logger.isTraceEnabled) { logger.trace("SubTreeRootNode without pathElem") }
            null
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
        return GtvMerkleProofTree(rootElement) // TODO: Q77: We don't know the size, since we don't have the original GTV struct at hand. This shouldn't be a problem since we are not gonna put this in cache anyway
    }

    private fun deserializeSub(currentSerializedArrayGtv: GtvArray): MerkleProofElement {

        val head = currentSerializedArrayGtv[0]
        val typeCode = (head as GtvInteger).asInteger()
        val secondElement = currentSerializedArrayGtv[1]
        return when (typeCode) {
            SERIALIZATION_HASH_LEAF_TYPE -> {
                val byteArray = secondElement as GtvByteArray
                ProofHashedLeaf(byteArray.bytearray)
            }
            SERIALIZATION_VALUE_LEAF_TYPE -> {
                val pathElem = deserializePathElement(secondElement)!! // If it's null, it means the proof structure is incorrect.
                val gtvContent = currentSerializedArrayGtv[2]
                val nrOfBytes = gtvContent.nrOfBytes()
                ProofValueGtvLeaf(gtvContent, nrOfBytes, pathElem)
            }
            SERIALIZATION_NODE_TYPE -> {
                val left: MerkleProofElement = deserializeSub(secondElement as GtvArray)
                val right: MerkleProofElement = deserializeSub(currentSerializedArrayGtv[2] as GtvArray)
                ProofNodeSimple(left, right)
            }
            SERIALIZATION_ARRAY_TYPE ->  {
                val size = (secondElement as GtvInteger).integer.toInt()
                val pathElem = deserializePathElement(currentSerializedArrayGtv[2])
                val left: MerkleProofElement = deserializeSub(currentSerializedArrayGtv[3] as GtvArray)
                val right: MerkleProofElement = deserializeSub(currentSerializedArrayGtv[4] as GtvArray)
                ProofNodeGtvArrayHead(size, left, right, pathElem)
            }
            SERIALIZATION_DICT_TYPE ->  {
                val size = (secondElement as GtvInteger).integer.toInt()
                val pathElem = deserializePathElement(currentSerializedArrayGtv[2])
                val left: MerkleProofElement = deserializeSub(currentSerializedArrayGtv[3] as GtvArray)
                val right: MerkleProofElement = deserializeSub(currentSerializedArrayGtv[4] as GtvArray)
                ProofNodeGtvDictHead(size, left, right, pathElem)
            }
            else -> throw IllegalStateException("Should handle the type $typeCode")
        }
    }

    /**
     * @return a path element or null (root Gtv collection will have null as path element).
     */
    private fun deserializePathElement(src: Gtv): SearchableGtvPathElement? {
        return when(src) {
            is GtvString -> {
                if (logger.isTraceEnabled) { logger.trace("Deserialize proof path dict key : ${src.string}") }
                DictGtvPathElement(null, src.string)
            }
            is GtvInteger -> {
                val l = src.integer.toLong()
                if (l != GtvMerkleBasics.UNKNOWN_COLLECTION_POSITION) {
                    if (logger.isTraceEnabled) { logger.trace("Deserialize proof path array path index : $l") }
                    ArrayGtvPathElement(null, l.toInt())
                } else {
                    if (logger.isTraceEnabled) { logger.trace("No path element for this GTV: ${src.type}") }
                    null
                }
            }
            else -> throw UserMistake("The GTV at this position must be either GtvInteger or GtvString, but is: $src")
        }
    }
}