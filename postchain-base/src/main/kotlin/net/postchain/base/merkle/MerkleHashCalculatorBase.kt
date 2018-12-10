package net.postchain.base.merkle

import net.postchain.gtx.GTXValue
import net.postchain.gtx.IntegerGTXValue
import net.postchain.gtx.encodeGTXValue

/**
 * This should be the serialization we always will use (unless in testing)
 *
 * @param gtxValue to serialize
 * @return the byte array containing serialized data
 */
fun serializeGTXValueToByteArary(gtxValue: GTXValue): ByteArray {
    var byteArr: ByteArray? = null
    when (gtxValue) {
        is IntegerGTXValue -> {
            byteArr = encodeGTXValue(gtxValue)
        }
        else -> {
            throw NotImplementedError("This is a todo") // TODO: Fix
        }
    }
    return byteArr!!
}

// TODO: replace
fun dummyFun(bArr: ByteArray): Hash {
    val retArr = ByteArray(bArr.size)
    var pos = 0
    for (b: Byte in bArr.asIterable()) {
        retArr[pos] = b.inc()
        pos++
    }
    return retArr
}

class MerkleHashCalculatorBase: MerkleHashCalculator() {

    /**
     * Leafs hashes are prefixed to tell them apart from internal nodes
     *
     * @param gtxValue The leaf
     * @return Returns the hash of the leaf.
     */
    override fun calculateLeafHash(gtxValue: GTXValue): Hash {
        return calculateHashOfGtxInternal(gtxValue, ::serializeGTXValueToByteArary, ::dummyFun)
    }


    /**
     * Internal nodes' hashes are prefixed to tell them apart from leafs.
     *
     * @param hashLeft The hash of the left sub tree
     * @param hashRight The hash of the right sub tree
     * @return Returns the hash of two combined hashes.
     */
    override fun calculateNodeHash(hashLeft: Hash, hashRight: Hash): Hash {
        return calculateNodeHashInternal(hashLeft, hashRight, ::dummyFun)
    }

}