// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.merkle

import net.postchain.base.merkle.proof.MerkleHashSummary
import net.postchain.gtv.Gtv
import java.util.*


data class CacheElement(val orgGtv: Gtv, val merkleHashSummary: MerkleHashSummary, var age: Long) {

    /**
     * Use when you want the time to be "now"
     */
    constructor(orgGtv: Gtv, merkleHashSummary: MerkleHashSummary): this(orgGtv, merkleHashSummary, System.currentTimeMillis() )


    // We have the merkle hash (which is unique) so we only have to look at that
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheElement

        if (!Arrays.equals(merkleHashSummary.merkleHash, other.merkleHashSummary.merkleHash)) return false

        return true
    }

    // We have the merkle hash (which is unique) so we only have to look at that
    override fun hashCode(): Int {
        return Arrays.hashCode(merkleHashSummary.merkleHash)
    }

}

/**
 * A set of Merkle Hashes that share the property that the corresponding [Gtv] generates the same (Java) hash code
 */
data class MerkleHashSetWithSameGtvJavaHashCode(val internalSet: MutableSet<CacheElement>) {

    companion object {

        const val AGE_SIZE_BYTES = 4 // Int
        const val SUMMARY_NO_OF_BYTES_SIZE_BYTES = 4 // Int
        const val CARRIER_SIZE_BYTES = 33 // 32 hash + 1 prefix
        const val OVERHEAD_SIZE_BYTES = AGE_SIZE_BYTES + SUMMARY_NO_OF_BYTES_SIZE_BYTES + CARRIER_SIZE_BYTES

        /**
         * @return a new [MerkleHashSetWithSameGtvJavaHashCode] set with one element in it.
         */
        fun build(gtvSrc: Gtv, merkleHashSummary: MerkleHashSummary): MerkleHashSetWithSameGtvJavaHashCode {
            val newElement = CacheElement(gtvSrc, merkleHashSummary)
            val mutSet = mutableSetOf(newElement)
            return MerkleHashSetWithSameGtvJavaHashCode(mutSet)
        }
    }

    /**
     * @return first entry that equals the given [Gtv] structure
     */
    fun findFromGtv(gtvSrc: Gtv): MerkleHashSummary? {
        for (cacheElement in internalSet) {
            if (cacheElement.orgGtv == gtvSrc) {
                // Found it!
                cacheElement.age = System.currentTimeMillis() // Update the age, so it won't get pruned
                return cacheElement.merkleHashSummary
            }
        }
        return null
    }

    /**
     * Will add a new [CacheElement] to the set
     */
    fun addToSet(gtvSrc: Gtv, merkleHashSummary: MerkleHashSummary) {
        val newElement = CacheElement(gtvSrc, merkleHashSummary)
        this.internalSet.add(newElement)
    }

    /**
     * Remove the [CacheElement] with the corresponding timestamp
     *
     * @return the pruned object
     */
    fun prune(elementToRemove: CacheElement): CacheElement? {
        if (internalSet.remove(elementToRemove)) {
            // Found
            return elementToRemove
        } else {
            // Not found
            return null
        }
    }

}