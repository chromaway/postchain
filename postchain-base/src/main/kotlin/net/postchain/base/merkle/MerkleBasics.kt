package net.postchain.base.merkle

import net.postchain.base.CryptoSystem
import net.postchain.core.ProgrammerMistake


typealias Hash = ByteArray

object MerkleBasics {

    /**
     * Use this to represent a hash of an empty element (in a tree, typically)
     */
    val EMPTY_HASH = ByteArray(32) // Just zeros

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
}