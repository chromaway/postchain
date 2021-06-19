// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.merkle

import net.postchain.base.CryptoSystem
import net.postchain.common.data.Hash

abstract class BinaryNodeHashCalculator(val cryptoSystem: CryptoSystem?) {

    abstract fun calculateNodeHash(prefix: Byte, hashLeft: Hash, hashRight: Hash): Hash

    /**
     * We smack on the prefix before hashing.
     *
     * @param prefix the one byte prefix to use
     * @param hashLeft The hash of the left sub tree
     * @param hashRight The hash of the right sub tree
     * @param hashFun The only reason we pass the function as a parameter is to simplify testing.
     * @return the hash of two combined hashes.
     */
    protected fun calculateNodeHashInternal(prefix: Byte, hashLeft: Hash, hashRight: Hash, hashFun: (ByteArray, CryptoSystem?) -> Hash): Hash {
        val byteArraySum = byteArrayOf(prefix) + hashLeft + hashRight
        return hashFun(byteArraySum, cryptoSystem)
    }
}