package net.postchain.gtv

import net.postchain.base.merkle.proof.SERIALIZATION_HASH_LEAF_TYPE
import net.postchain.base.merkle.proof.SERIALIZATION_NODE_TYPE
import net.postchain.base.merkle.proof.SERIALIZATION_VALUE_LEAF_TYPE
import net.postchain.gtv.merkle.proof.SERIALIZATION_ARRAY_TYPE
import net.postchain.gtv.merkle.proof.SERIALIZATION_DICT_TYPE
import net.postchain.common.hexStringToByteArray

object GtvProofTreeTestHelper {


    /**
     * Will traverse a GTV block proof tree recursively until it finds the TX hash to be proven (given as param)
     *
     * @param hashToProve is the value to be proven (that should be somewhere in this proof
     * @return true if [hashToProve] is found
     */
    fun findHashInBlockProof(hashToProve: ByteArray, list: List<Any>): Boolean {
        val firstElem: Any = list[0]
        val code = when (firstElem) {
            is Float -> firstElem.toLong()
            is Double -> firstElem.toLong()
            is Int -> firstElem.toLong()
            is Long -> firstElem
            else -> throw IllegalStateException("Not expected to find type: ${firstElem} as first element of array in block proof")
        }
        when (code) {
            SERIALIZATION_VALUE_LEAF_TYPE -> { // 101
                // Found it!
                return true
            }
            SERIALIZATION_HASH_LEAF_TYPE -> { // 100
                // Nothing interesting here, move on
                return false
            }
            SERIALIZATION_ARRAY_TYPE -> {  // 103
                val leftSide = list[3] as List<Any>
                if (findHashInBlockProof(hashToProve, leftSide)) {
                    return true
                } else {
                    val rightSide = list[4] as List<Any>
                    if (findHashInBlockProof(hashToProve, rightSide)) {
                        return true
                    }
                }
            }
            SERIALIZATION_NODE_TYPE -> { // 102 (intermediary node, not sure why it's used in proof)
                val leftSide = list[1] as List<Any>
                if (findHashInBlockProof(hashToProve, leftSide)) {
                    return true
                } else {
                    val rightSide = list[2] as List<Any>
                    if (findHashInBlockProof(hashToProve, rightSide)) {
                        return true
                    }
                }
            }
            SERIALIZATION_DICT_TYPE -> throw IllegalStateException("Dict not expected in a block proof")
            else -> throw IllegalStateException("Don't know this code: $code")
        }
        throw IllegalStateException("How the hell did we get here?")
    }


    /**
     * Will traverse a GTV structure and replace every [GtvString] with [GtvByteArray]
     *
     * Note: It only makes sense to call this method SOMETIMES, for example when we are looking at a
     * block proof (where we only have hashes).
     */
    fun translateGtvStringToGtvByteArray(gtv: Gtv): Gtv {
        return when (gtv) {
            is GtvString -> GtvByteArray(gtv.string.hexStringToByteArray())
            is GtvArray -> {
                // Translate all content
                val retList = mutableListOf<Gtv>()
                for (elem in gtv.array) {
                    val ret = translateGtvStringToGtvByteArray(elem)
                    retList.add(ret)
                }
                GtvArray(retList.toTypedArray())
            }
            is GtvDictionary -> throw IllegalArgumentException("translateGtvStringToGtvByteArray() - does not handle dicts yet")
            else -> gtv
        }
    }

}