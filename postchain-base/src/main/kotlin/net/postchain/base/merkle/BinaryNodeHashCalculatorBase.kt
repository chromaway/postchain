package net.postchain.base.merkle

import net.postchain.base.CryptoSystem

/**
 * This is the implementation that is intended to be used in production
 */
class BinaryNodeHashCalculatorBase(cryptoSystem: CryptoSystem?): BinaryNodeHashCalculator(cryptoSystem) {

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
        return prefixBA + calculateNodeHashNoPrefixInternal(hashLeft, hashRight, MerkleBasics::hashingFun)
    }
}