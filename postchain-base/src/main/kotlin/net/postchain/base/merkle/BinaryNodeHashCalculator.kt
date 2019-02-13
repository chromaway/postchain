package net.postchain.base.merkle

import net.postchain.base.CryptoSystem
import net.postchain.base.merkle.proof.MerkleHashCarrier

abstract class BinaryNodeHashCalculator(val cryptoSystem: CryptoSystem?) {

    /**
     * Same as above but with prefix
     */
    abstract fun calculateNodeHash(prefix: Byte, hashLeft: MerkleHashCarrier, hashRight: MerkleHashCarrier): MerkleHashCarrier

    /**
     * @param hashLeft The hash of the left sub tree
     * @param hashRight The hash of the right sub tree
     * @param hashFun The only reason we pass the function as a parameter is to simplify testing.
     * @return the hash of two combined hashes.
     */
    protected fun calculateNodeHashNoPrefixInternal(hashLeft: Hash, hashRight: Hash, hashFun: (ByteArray, CryptoSystem?) -> Hash): Hash {
        val byteArraySum = hashLeft + hashRight
        return hashFun(byteArraySum, cryptoSystem) // Adding the prefx at the last step
    }
}