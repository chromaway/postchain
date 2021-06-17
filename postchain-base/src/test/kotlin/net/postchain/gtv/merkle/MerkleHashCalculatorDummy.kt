// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.merkle

import net.postchain.base.CryptoSystem
import net.postchain.common.data.Hash
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.base.merkle.TreeHelper
import net.postchain.gtv.*
import java.nio.charset.Charset


/**
 * We create a dummy serializer so that we can use for tests
 * It truncates an Int to one byte
 */
fun dummySerializatorFun(iGtv: Gtv): ByteArray {
    when (iGtv) {
        is GtvInteger -> {
            val i: Long = iGtv.integer.toLong()
            if (i > 127 && i > -1) {
                throw IllegalArgumentException("Test integers should be positive and should not be bigger than 127: $i")
            } else {
                val b: Byte = i.toByte()
                return byteArrayOf(b)
            }
        }
        is GtvString -> {
            val str = iGtv.string
            val byteArr = str.toByteArray(Charset.defaultCharset()) // TODO: Do we need to think about charset?
            println("Dummy serializer:  string: $str becomes bytes: " + TreeHelper.convertToHex(byteArr))
            return byteArr
        }
        is GtvByteArray -> {
            return iGtv.asByteArray() // No need to do anything
        }
        else -> {
            throw IllegalArgumentException("We don't use any other than Integers, Strings and ByteArray for these tests: ${iGtv.type.name}")
        }
    }
}

/**
 * A simple dummy hashing function that's easy to test
 * It only adds 1 to all bytes in the byte array.
 */
fun dummyAddOneHashFun(bArr: ByteArray, cryptoSystem: CryptoSystem?): Hash {
    val retArr = ByteArray(bArr.size)
    var pos = 0
    for (b: Byte in bArr.asIterable()) {
        retArr[pos] = b.inc()
        pos++
    }
    return retArr
}

/**
 * The "dummy" version is a real calculator, but it uses simplified versions of
 * serializations and hashing
 *
 * @property memoization is possible to override with some other version (for example that prunes more often)
 */
class MerkleHashCalculatorDummy(): MerkleHashCalculator<Gtv>(null) {


    override fun calculateLeafHash(value: Gtv): Hash {
        val hash = calculateHashOfValueInternal(value, ::dummySerializatorFun, ::dummyAddOneHashFun)
        //println("Hex: " + TreeHelper.convertToHex(hash))
        return hash
    }

    override fun calculateNodeHash(prefix: Byte, hashLeft: Hash, hashRight: Hash): Hash {
        return calculateNodeHashInternal(prefix ,hashLeft, hashRight, ::dummyAddOneHashFun)
    }

    override fun isContainerProofValueLeaf(value: Gtv): Boolean {
        return when (value) {
            is GtvCollection -> true
            is GtvPrimitive -> false
            else -> throw IllegalStateException("The type is neither collection or primitive. type: ${value.type} ")
        }
    }

}