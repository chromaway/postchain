package net.postchain.gtx.merkle.proof

import net.postchain.base.merkle.proof.*
import net.postchain.gtx.merkle.GtxMerkleBasics.HASH_PREFIX_NODE_GTX_ARRAY
import net.postchain.gtx.merkle.GtxMerkleBasics.HASH_PREFIX_NODE_GTX_DICT
import net.postchain.gtx.*


const val SERIALIZATION_ARRAY_TYPE: Long = 103
const val SERIALIZATION_DICT_TYPE: Long = 104

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


    /**
     * In this case the implementation is trivial. We already have a value of the
     * correct type, so let's return it.
     */
    fun serializeValueLeafToGtx(valueLeaf: GTXValue): GTXValue {
        return valueLeaf
    }


    fun serializeToGtxInternal(currentElement: MerkleProofElement): ArrayGTXValue {
        return when (currentElement) {
            is ProofHashedLeaf -> {
                val tail = ByteArrayGTXValue(currentElement.hash)
                val head = IntegerGTXValue(SERIALIZATION_HASH_LEAF_TYPE)
                val arr: Array<GTXValue> = arrayOf(head, tail)
                ArrayGTXValue(arr)
            }
            is ProofValueLeaf<*> -> {
                val tail = serializeValueLeafToGtx(currentElement.content as GTXValue)
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
