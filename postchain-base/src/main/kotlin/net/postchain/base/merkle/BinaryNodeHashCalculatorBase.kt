package net.postchain.base.merkle

import net.postchain.base.CryptoSystem
import net.postchain.base.merkle.proof.MerkleHashCarrier

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
    override fun calculateNodeHash(prefix: Byte, hashLeft: MerkleHashCarrier, hashRight: MerkleHashCarrier): MerkleHashCarrier {
        return MerkleHashCarrier(prefix, calculateNodeHashNoPrefixInternal(hashLeft.getHashWithPrefix(), hashRight.getHashWithPrefix(), MerkleBasics::hashingFun))
    }
}