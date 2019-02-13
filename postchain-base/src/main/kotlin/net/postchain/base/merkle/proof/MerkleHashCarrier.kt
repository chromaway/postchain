package net.postchain.base.merkle.proof

import net.postchain.base.merkle.Hash
import java.util.*

/**
 * @property merkleHashCarrier holds the calculated merkle hash
 * @property nrOfBytes is the size in bytes of the original structure we were hashing
 */
data class MerkleHashSummary(val merkleHashCarrier: MerkleHashCarrier, val nrOfBytes: Int) {

    /**
     * @return The array with the prefix padded
     */
    fun getHashWithPrefix(): ByteArray {
        return merkleHashCarrier.getHashWithPrefix()
    }

    /**
     * @return The array without the prefix padded
     */
    fun getHashWithoutPrefix(): ByteArray {
        return merkleHashCarrier.hash
    }

}

/**
 * @property prefix is the prefix that should be used in front of the hash if we use this to calculate a new hash
 * @property hash is the calculated merkle hash
 */
data class MerkleHashCarrier(val prefix: Byte, val hash: Hash) {

    companion object {

        fun build(byteArr: ByteArray): MerkleHashCarrier {
            val tmpPrefix = byteArr[0]
            val tmpTail: ByteArray = byteArr.drop(1).toByteArray()

            return MerkleHashCarrier(tmpPrefix, tmpTail)
        }
    }


    /**
     * @return The array with the prefix padded
     */
    fun getHashWithPrefix(): ByteArray {
        val prefixBA = byteArrayOf(prefix)
        return prefixBA + hash
    }

    /**
     * @return The array without the prefix padded
     */
    fun getHashWithoutPrefix(): ByteArray {
        return hash
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MerkleHashCarrier

        if (prefix != other.prefix) return false
        if (!Arrays.equals(hash, other.hash)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prefix.toInt()
        result = 31 * result + Arrays.hashCode(hash)
        return result
    }
}