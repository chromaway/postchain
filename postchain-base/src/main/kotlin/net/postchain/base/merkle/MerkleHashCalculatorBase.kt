package net.postchain.base.merkle

import net.postchain.base.CryptoSystem
import net.postchain.core.ProgrammerMistake
import net.postchain.gtx.*

/**
 * This should be the serialization we use in production
 *
 * @param gtxValue to serialize
 * @return the byte array containing serialized data
 */
fun serializeGTXValueToByteArary(gtxValue: GTXValue): ByteArray {
    return when (gtxValue) {
        is GTXNull ->           encodeGTXValue(gtxValue)
        is IntegerGTXValue ->   encodeGTXValue(gtxValue)
        is StringGTXValue ->    encodeGTXValue(gtxValue)
        is ByteArrayGTXValue -> encodeGTXValue(gtxValue)
        is ArrayGTXValue ->     throw ProgrammerMistake("GTXValue is an array (We should have transformed all collection-types to trees by now)")
        is DictGTXValue ->      throw ProgrammerMistake("GTXValue is an dict (We should have transformed all collection-types to trees by now)")
        else -> {
            // TODO: Log a warning here? We don't know what this is!
            encodeGTXValue(gtxValue) // Hope for the best
        }
    }
}

/**
 * This should be the hashing function we use in production
 *
 * @param bArr is the data to hash
 * @param cryptoSystem used to get the hash function
 * @return the hash we calculated
 */
fun hashingFun(bArr: ByteArray, cryptoSystem: CryptoSystem?): Hash {
    if (cryptoSystem == null) {
        throw ProgrammerMistake("In this case we need the CryptoSystem to calculate the hash")
    }  else {
        return cryptoSystem!!.digest(bArr)
    }
}

class MerkleHashCalculatorBase(cryptoSystem: CryptoSystem): MerkleHashCalculator(cryptoSystem) {



    /**
     * Leafs hashes are prefixed to tell them apart from internal nodes
     *
     * @param gtxValue The leaf
     * @return Returns the hash of the leaf.
     */
    override fun calculateLeafHash(gtxValue: GTXValue): Hash {
        return calculateHashOfGtxInternal(gtxValue, ::serializeGTXValueToByteArary, ::hashingFun)
    }


    /**
     * Internal nodes' hashes are prefixed to tell them apart from leafs.
     *
     * @param hashLeft The hash of the left sub tree
     * @param hashRight The hash of the right sub tree
     * @return Returns the hash of two combined hashes.
     */
    //override fun calculateNodeHash(hashLeft: Hash, hashRight: Hash): Hash {
    //    return calculateNodeHashNoPrefix(hashLeft, hashRight, ::hashingFun)
    //}

    /**
     * Internal nodes' hashes are prefixed to tell them apart from leafs.
     *
     * @param prefix What byte to put in front of the hash
     * @param hashLeft The hash of the left sub tree
     * @param hashRight The hash of the right sub tree
     * @return Returns the hash of two combined hashes.
     */
    override fun calculateNodeHash(prefix: Byte, hashLeft: Hash, hashRight: Hash): Hash {
        val prefixBA = byteArrayOf(prefix)
        return prefixBA + calculateNodeHashNoPrefix(hashLeft, hashRight, ::hashingFun)
    }

}