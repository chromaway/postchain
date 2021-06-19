// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.merkle.proof

import net.postchain.common.data.Hash
import java.util.*

/**
 * @property merkleHash is the calculated merkle hash
 * @property nrOfBytes is the size in bytes of the original structure we were hashing
 */
data class MerkleHashSummary(val merkleHash: Hash, val nrOfBytes: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MerkleHashSummary

        if (!Arrays.equals(merkleHash, other.merkleHash)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(merkleHash)
    }
}
