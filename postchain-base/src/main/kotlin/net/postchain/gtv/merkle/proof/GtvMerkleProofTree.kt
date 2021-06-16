// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.merkle.proof

import net.postchain.common.data.Hash
import net.postchain.base.merkle.MerkleBasics.UNKNOWN_SIZE_IN_BYTE
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.base.merkle.proof.*
import net.postchain.gtv.*
import net.postchain.gtv.merkle.GtvMerkleBasics
import net.postchain.gtv.merkle.GtvMerkleBasics.HASH_PREFIX_NODE_GTV_ARRAY
import net.postchain.gtv.merkle.GtvMerkleBasics.HASH_PREFIX_NODE_GTV_DICT
import net.postchain.gtv.merkle.GtvMerkleBasics.UNKNOWN_COLLECTION_POSITION
import net.postchain.gtv.path.SearchableGtvPathElement


const val SERIALIZATION_ARRAY_TYPE: Long = 103
const val SERIALIZATION_DICT_TYPE: Long = 104

/**
 * Like the superclass [ProofValueLeaf] but for [Gtv].
 *
 * @property content (see super)
 * @property sizeInBytes (see super)
 * @property pathElem is the path element that tells us how to find this element in the surrounding collection
 */
class ProofValueGtvLeaf(
        content: Gtv,
        sizeInBytes: Int,
        val pathElem: SearchableGtvPathElement
): ProofValueLeaf<Gtv>(content, sizeInBytes)

/**
 * Represents a proof node that once was the head of a Gtv array.
 *
 * @property size is number of elements in the original array.
 * @property left (see super)
 * @property right (see super)
 * @property pathElem tells us the position of the [GtvArray] in the collection above this one.
 *        Note: pathElem is only null if this is the root element in the proof (since there is no collection above).
 *
 */
class ProofNodeGtvArrayHead(
        val size: Int,
        left: MerkleProofElement,
        right: MerkleProofElement,
        val pathElem: SearchableGtvPathElement? = null
): ProofNode(HASH_PREFIX_NODE_GTV_ARRAY, left, right)

/**
 * Represents a proof node that once was the head of a Gtv dict.
 *
 * @property size number of key-value pairs. Note: In the case of dictionary, we don't strictly NEED to preserve the
 *        size, since we will use the key to find our value, and a dictionary of size == 1
 *        (only our key/value pair in it) will serve just as well, but we save the size anyway for symmetry.
 * @property left (see super)
 * @property right (see super)
 * @property pathElem tells us the position of the [GtvDictionary] in the collection above this one.
 *        Note: pathElem is only null if this is the root element in the proof (since there is no collection above).
 */
class ProofNodeGtvDictHead(
        val size: Int,
        left: MerkleProofElement,
        right: MerkleProofElement,
        val pathElem: SearchableGtvPathElement? = null
): ProofNode(HASH_PREFIX_NODE_GTV_DICT, left, right)


/**
 * See [MerkleProofTree] for documentation
 *
 * @property root is the root of the proof
 * @property totalNrOfBytes is the size in bytes of the original [Gtv] structure (sometimes we don't have the size, e.g. after deserialization)
 */
class GtvMerkleProofTree(root: MerkleProofElement, totalNrOfBytes: Int = UNKNOWN_SIZE_IN_BYTE ):
        MerkleProofTree<Gtv>(root, totalNrOfBytes) {

    /**
     * Note that we have our own primitive serialization format. It is based on arrays that begins with an integer.
     * The integer tells us what the content is.
     * 0 -> just a hash
     * 1 -> value to be proven
     * 2 -> a node
     * (we can add more in sub classes)
     */
    fun serializeToGtv(): GtvArray {
        return serializeToGtvInternal(this.root)
    }


    /**
     * In this case the implementation is trivial. We already have a value of the
     * correct type, so let's return it.
     */
    fun serializeValueLeafToGtv(valueLeaf: Gtv): Gtv {
        return valueLeaf
    }


    fun serializeToGtvInternal(currentElement: MerkleProofElement): GtvArray {
        return when (currentElement) {
            is ProofHashedLeaf -> {
                val tail = GtvByteArray(currentElement.merkleHash)
                val head = GtvInteger(SERIALIZATION_HASH_LEAF_TYPE)
                val arr: Array<Gtv> = arrayOf(head, tail)
                GtvArray(arr)
            }
            is ProofValueGtvLeaf -> {
                val tail = serializeValueLeafToGtv(currentElement.content)
                val head = GtvInteger(SERIALIZATION_VALUE_LEAF_TYPE)
                val position = serializePathElement(currentElement.pathElem)
                val arr: Array<Gtv> = arrayOf(head, position, tail)
                GtvArray(arr)
            }
            is ProofNodeSimple -> {
                val tail1 = serializeToGtvInternal(currentElement.left)
                val tail2 = serializeToGtvInternal(currentElement.right)
                val head = GtvInteger(SERIALIZATION_NODE_TYPE)
                val arr: Array<Gtv> = arrayOf(head, tail1, tail2)
                GtvArray(arr)
            }
            is ProofNodeGtvArrayHead -> {
                val tail1 = serializeToGtvInternal(currentElement.left)
                val tail2 = serializeToGtvInternal(currentElement.right)
                val size = GtvInteger(currentElement.size.toLong())
                val position = serializePathElement(currentElement.pathElem)
                val head = GtvInteger(SERIALIZATION_ARRAY_TYPE)
                val arr: Array<Gtv> = arrayOf(head, size, position, tail1, tail2)
                GtvArray(arr)
            }
            is ProofNodeGtvDictHead -> {
                val tail1 = serializeToGtvInternal(currentElement.left)
                val tail2 = serializeToGtvInternal(currentElement.right)
                val size = GtvInteger(currentElement.size.toLong())
                val position = serializePathElement(currentElement.pathElem)
                val head = GtvInteger(SERIALIZATION_DICT_TYPE)
                val arr: Array<Gtv> = arrayOf(head, size, position, tail1, tail2)
                GtvArray(arr)
            }
            else -> throw IllegalStateException("This type should have been taken care of: $currentElement")
        }
    }

    private fun serializePathElement(pathElem: SearchableGtvPathElement?): Gtv {
        return if (pathElem != null) {
            pathElem.buildGtv()
        } else {
            GtvInteger(UNKNOWN_COLLECTION_POSITION )
        }
    }

}


/**
 * Calculates the merkle root hash of the proof structure.
 *
 * @param calculator describes the method we use for hashing and serialization
 * @return the merkle root hash
 */
fun GtvMerkleProofTree.merkleHash(calculator: MerkleHashCalculator<Gtv>): Hash {
    return this.merkleHashSummary(calculator).merkleHash
}

/**
 * Calculates the merkle root hash of the proof structure.
 *
 * @param calculator describes the method we use for hashing and serialization
 * @return the merkle root hash summary
 */
fun GtvMerkleProofTree.merkleHashSummary(calculator: MerkleHashCalculator<Gtv>): MerkleHashSummary {

    val summaryFactory = GtvMerkleBasics.getGtvMerkleHashSummaryFactory()
    return summaryFactory.calculateMerkleRoot(this, calculator)
}

/**
 * @return a virtual GTV version of the original [Gtv] (based only on the info we could find in the proof,
 *        so all hashed values will be "null")
 */
fun GtvMerkleProofTree.toGtvVirtual(): GtvVirtual {
    val virtualFactory = GtvMerkleBasics.getGtvVirtualFactory()
    return virtualFactory.buildGtvVirtual(this)
}
