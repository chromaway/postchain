// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.merkle

import net.postchain.base.CryptoSystem
import net.postchain.common.data.Hash


/**
 * Abstract class responsible for calculating hashes and serialization.
 * Can calculate hashes of leaves and nodes.
 *
 * Note: We make this class abstract so we can use a dummy version during test (this makes tests easier to understand).
 */
abstract class MerkleHashCalculator<T>(cryptoSystem: CryptoSystem?): BinaryNodeHashCalculator(cryptoSystem) {

    /**
     * Leaf hashes are prefixed to tell them apart from internal nodes.
     *
     * @param value The leaf
     * @return the hash of a leaf.
     */
    abstract fun calculateLeafHash(value: T): Hash


    /**
     * We will smack on the "leaf prefix" to the binary data before the hashing function is run.
     *
     * @param valueToHash The leaf
     * @param serializeFun The only reason we pass the function as a parameter is to simplify testing.
     * @param hashFun The only reason we pass the function as a parameter is to simplify testing.
     * @return the hash of the valueToHash.
     */
    protected fun calculateHashOfValueInternal(
            valueToHash: T,
            serializeFun: (T) -> ByteArray,
            hashFun: (ByteArray, CryptoSystem?) -> Hash
    ): Hash {
        val byteArr: ByteArray = byteArrayOf(MerkleBasics.HASH_PREFIX_LEAF) + serializeFun(valueToHash)
        return hashFun(byteArr, cryptoSystem)
    }

    /**
     * Note: We must override this method if the value can be a container (as in the case with GTV).
     *
     * @return True if the value can hold other values (i.e. if it is a container of sorts).
     */
    open fun isContainerProofValueLeaf(value: T): Boolean = false

}