package net.postchain.base.merkle

import net.postchain.base.CryptoSystem
import net.postchain.base.merkle.proof.MerkleProofTree


/**
 * Abstract class responsible for calculating hashes and serialization.
 * Can calculate hashes of leaves and nodes.
 */
abstract class MerkleHashCalculator<T, TPath>(cryptoSystem: CryptoSystem?): BinaryNodeHashCalculator(cryptoSystem) {

    /**
     * Leaf hashes are prefixed to tell them apart from internal nodes.
     *
     * @param value The leaf
     * @return the hash of a leaf.
     */
    abstract fun calculateLeafHash(value: T): Hash


    /**
     * @param valueToHash The leaf
     * @param serializeFun The only reason we pass the function as a parameter is to simplify testing.
     * @param hashFun The only reason we pass the function as a parameter is to simplify testing.
     * @return the hash of the valueToHash.
     */
    protected fun calculateHashOfValueInternal(valueToHash: T, serializeFun: (T) -> ByteArray, hashFun: (ByteArray, CryptoSystem?) -> Hash): Hash {
        var byteArr: ByteArray = serializeFun(valueToHash)
        return byteArrayOf(MerkleBasics.HASH_PREFIX_LEAF) + hashFun(byteArr, cryptoSystem)
    }

    /**
     * Note: We must override this method if the value can be a container.
     *
     * @return True if the value can hold other values (i.e. if it is a container of sorts).
     */
    open fun isContainerProofValueLeaf(value: T): Boolean = false

    /**
     * Note; We must override this method if a value can be a container
     *
     * @return a sub root of a [MerkleProofTree] built from the value (where the value is expected to be a container)
     */
    open fun buildTreeFromContainerValue(value: T): MerkleProofTree<T, TPath> {
        throw IllegalStateException("A value $value cannot be a container")
    }

}