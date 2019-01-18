package net.postchain.base.merkle.proof

import net.postchain.base.merkle.MerkleBasics.HASH_PREFIX_NODE_GTX_ARRAY
import net.postchain.base.merkle.MerkleBasics.HASH_PREFIX_NODE_GTX_DICT
import net.postchain.gtx.*


const val SERIALIZATION_ARRAY_TYPE: Long = 3
const val SERIALIZATION_DICT_TYPE: Long = 4

/**
 * Represents a proof node that once was the head of a GTX array.
 *
 * Note: We keep the size in case we need to use a [GTXPath] to find a value
 */
class ProofNodeGtxArrayHead(val size: Int, left: MerkleProofElement, right: MerkleProofElement): ProofNode(HASH_PREFIX_NODE_GTX_ARRAY, left, right)

/**
 * Represents a proof node that once was the head of a GTX dict.
 *
 * Note: In the case of dictionary, we don't strictly NEED to preserve the size, since we will use the key to find
 * our value, and a dictionary of size == 1 (only our key/value pair in it) will serve just as well, but we save the
 * size anyway for symmetry.
 */
class ProofNodeGtxDictHead(val size: Int, left: MerkleProofElement, right: MerkleProofElement): ProofNode(HASH_PREFIX_NODE_GTX_DICT, left, right)

/**
 * See [MerkleProofTree] for documentation
 */
class GtxMerkleProofTree(root: MerkleProofElement): MerkleProofTree<GTXValue, GTXPath>(root) {

    /**
     * In this case the implementation is trivial. We already have a value of the
     * correct type, so let's return it.
     */
    override fun serializeValueLeafToGtx(valueLeaf: GTXValue): GTXValue {
        return valueLeaf
    }


    /**
     * "Other types" refers to [ProofNodeGtxArrayHead] and [ProofNodeGtxDictHead] in this case. We add the following integers to our
     * serialization policy: [SERIALIZATION_ARRAY_TYPE] and [SERIALIZATION_DICT_TYPE].
     */
    override fun serializeOtherTypes(currentElement: MerkleProofElement): ArrayGTXValue {

        return when (currentElement) {
            is ProofNodeGtxArrayHead -> {
                val tail1 = serializeToGtxInternal(currentElement.left)
                val tail2 = serializeToGtxInternal(currentElement.right)
                val size = IntegerGTXValue(currentElement.size.toLong())
                val head = IntegerGTXValue(SERIALIZATION_ARRAY_TYPE)
                val arr: Array<GTXValue> = arrayOf(head, size, tail1, tail2)
                ArrayGTXValue(arr)
            }
            is ProofNodeGtxDictHead -> {
                val tail1 = serializeToGtxInternal(currentElement.left)
                val tail2 = serializeToGtxInternal(currentElement.right)
                val size = IntegerGTXValue(currentElement.size.toLong())
                val head = IntegerGTXValue(SERIALIZATION_DICT_TYPE)
                val arr: Array<GTXValue> = arrayOf(head, size, tail1, tail2)
                ArrayGTXValue(arr)
            }
            else -> throw IllegalStateException("This type should have been taken care of: $currentElement")
        }
    }
}
