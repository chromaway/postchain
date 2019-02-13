package net.postchain.gtv.merkle

import mu.KLogging
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.base.merkle.proof.MerkleHashSummary
import net.postchain.gtv.Gtv
import net.postchain.gtv.merkle.proof.GtvMerkleProofTreeFactory


data class CacheElement(val orgGtv: Gtv, val merkleHashSummary: MerkleHashSummary, var age: Long) {

    /**
     * Use when you want the time to be "now"
     */
    constructor(orgGtv: Gtv, merkleHashSummary: MerkleHashSummary): this(orgGtv, merkleHashSummary, System.currentTimeMillis() )
}

/**
 * A set of Merkle Hashes that share the property that the corresponding [Gtv] generates the same (Java) hash code
 */
data class MerkleHashSetWithSameGtvJavaHashCode(val internalSet: MutableSet<CacheElement>) {

    companion object {

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

/**
 * To improve performance, we will cache the hash of a given [Gtv] object.
 * (Well look in the cache, and only if not found we'll calculate the hash)
 *
 * Note1: that there is a high risk for collisions on Java's hashCode(), so that is why we have to store a set of all
 * [Gtv]s with this hash code. This might be a threat to memory, so we need a way to prune the cache.
 *
 *
 */

object GtvMerkleHashCache {

    val defaultSizeOf100MB = 100000000 // TODO: Make this configerable
    val defaultNumberOfLookupsBeforePrune = 100 // Not that important, no need to configure

    val gtvMerkleHashMemoization = GtvMerkleHashMemoization(defaultNumberOfLookupsBeforePrune, defaultSizeOf100MB)

    fun findOrCalculateMerkleHash(gtvSrc: Gtv, calculator: MerkleHashCalculator<Gtv>): MerkleHashSummary {
        return gtvMerkleHashMemoization.findOrCalculateMerkleHash(gtvSrc, calculator)
    }
}

/**
 * Note1: Don't use this class, use the object (above). The reason we create a separate class called by the object
 * is that we want to test pruning using lower values.
 *
 * @property TRY_PRUNE_AFTER_THIS_MANY_LOOKUPS controls how frequently we will try to prune
 * @property MAX_CACHE_SIZE_BYTES controls how many bytes we allow the cache to grow to.
 *                  NOTE: The real amount of bytes consumed will be smaller, since we cache parts
 *                  of the tree many times over. Too hard to calculate real memory consumption, and don't want to
 *                  use java.lang.instrumentation (b/c probably too heavy)
 */
class GtvMerkleHashMemoization(val TRY_PRUNE_AFTER_THIS_MANY_LOOKUPS: Int ,val MAX_CACHE_SIZE_BYTES: Int) {

    companion object : KLogging()

    private val javaHashCodeToGtvSet_Map = HashMap<Int, MerkleHashSetWithSameGtvJavaHashCode>()

    private var totalSizeInBytes = 0

    // Some statistics
    private var cacheHits = 0L
    private var cacheMisses = 0L

    /**
     * @param gtvSrc the [Gtv] structure we need to get a merkle hash out of
     * @param calculator
     * @return the merkle root hash we need (either fetched from cache or calculated)
     */
    fun findOrCalculateMerkleHash(gtvSrc: Gtv, calculator: MerkleHashCalculator<Gtv>): MerkleHashSummary {

        var retMerkleHashSummary: MerkleHashSummary? = null
        val gtvSrcHashCode = gtvSrc.hashCode()

        val foundGtvSet = javaHashCodeToGtvSet_Map[gtvSrcHashCode]
        if (foundGtvSet != null) {
            retMerkleHashSummary = foundGtvSet.findFromGtv(gtvSrc)
        }

        if (retMerkleHashSummary != null) {
            synchronized(this) {
                cacheHits++
            }
        } else {
            // This should NOT be synchronized
            val calculatedMerkleHashSummary = calculateMerkleHash(gtvSrc, calculator)

            addToChache(gtvSrc, calculatedMerkleHashSummary, foundGtvSet, gtvSrcHashCode)

            retMerkleHashSummary = calculatedMerkleHashSummary
        }

        maybePrune()

        return retMerkleHashSummary
    }



    private fun calculateMerkleHash(gtvSrc: Gtv, calculator: MerkleHashCalculator<Gtv>): MerkleHashSummary {
        val factory = GtvBinaryTreeFactory()
        val proofFactory = GtvMerkleProofTreeFactory(calculator)

        // 1. Build Binary tree out of the Gtv object
        val binaryTree = factory.buildFromGtv(gtvSrc)

        // 2. Build ProofTree (in this case there is just 1 element in the tree)
        val proofTree = proofFactory.buildFromBinaryTree(binaryTree)

        // 3. Pick the root element (in this case no calculation needed here)
        return proofTree.calculateMerkleRoot(calculator)
    }

    /**
     * Note: this hash to be synchronized since we are updating the cache itself
     */
    @Synchronized
    private fun addToChache(gtvSrc: Gtv, calculatedMerkleHashSummary: MerkleHashSummary, foundGtvSet: MerkleHashSetWithSameGtvJavaHashCode?, javaHashCode: Int) {
        cacheMisses++
        if (foundGtvSet != null) {
            foundGtvSet.addToSet(gtvSrc, calculatedMerkleHashSummary)
        } else {
            val newSet = MerkleHashSetWithSameGtvJavaHashCode.build(gtvSrc, calculatedMerkleHashSummary)
            javaHashCodeToGtvSet_Map[javaHashCode] = newSet
        }
        totalSizeInBytes += calculatedMerkleHashSummary.nrOfBytes
    }


    /**
     * We check for pruning every [TRY_PRUNE_AFTER_THIS_MANY_LOOKUPS] lookup.
     *
     * We will only prune if total size of objects supercede [MAX_CACHE_SIZE_BYTES]
     *
     * Note: must be synchronized (since we don't want anyone updating the cache while we remove stuff)
     */
    @Synchronized
    private fun maybePrune() {
        var countGtvs = 0
        var didPrune = false
        try {
            if ((cacheHits + cacheMisses).rem(TRY_PRUNE_AFTER_THIS_MANY_LOOKUPS) == 0L) {
                logger.debug("Time to check if cache is too big.")

                // Let's see if we should prune
                if (totalSizeInBytes > MAX_CACHE_SIZE_BYTES) {
                    // 0. Begin
                    logger.info("----------------------------------------------------------------------------------")
                    logger.info("Begin pruning GtvMerkleHashMemoization")
                    logger.info("----------------------------------------------------------------------------------")
                    val begin = System.currentTimeMillis()
                    didPrune = true // Do this now, we might crash later

                    // 1. Build a structure where timestamp is key
                    val timeStampToHashMap = HashMap<Long, Pair<Int, CacheElement>>()
                    for (key in javaHashCodeToGtvSet_Map.keys) {
                        val tmpSet = javaHashCodeToGtvSet_Map[key]!!
                        for (element in tmpSet.internalSet) {
                            timeStampToHashMap[element.age] = Pair(key, element)
                        }
                    }

                    // 2. Remove oldest timestamps first
                    val allTimestamps: List<Long> = timeStampToHashMap.keys.sorted().asReversed()

                    val idealSizeInBytes = MAX_CACHE_SIZE_BYTES / 2 // Our goal is to make the cache half-empty so we don't have to do this often
                    var pruned = 0
                    while (totalSizeInBytes > idealSizeInBytes) {
                        val timestampToRemove = allTimestamps[pruned]
                        removeFromSet(timestampToRemove, timeStampToHashMap)
                        pruned++
                    }

                    // 3. Done
                    val end = System.currentTimeMillis()
                    val duration = end - begin
                    logger.info("----------------------------------------------------------------------------------")
                    logger.info("Cache pruned successfully down to size: $totalSizeInBytes , objects pruned: $pruned  in $duration ms.")
                    logger.info("----------------------------------------------------------------------------------")
                }
            }
        } catch (e: Exception) {
            // Shouldn't let the failed pruning ruin everything, let's log it and move on
            logger.error ("Pruning (size: $countGtvs) failed with error: ", e)
        } finally {
            if (didPrune) {
                System.gc()
            }
        }

    }

    /**
     * Remove the cached element with the timestamp from the corresponding set, and the entire set if it's empty
     *
     * (Remember to adjust the total size of the cache)
     */
    @Synchronized
    private fun removeFromSet(timestampToRemove: Long, timeStampToHashMap: HashMap<Long, Pair<Int, CacheElement>>)  {
        val tmpHashCodeAndHashPair = timeStampToHashMap[timestampToRemove]!!
        val javaHashCode = tmpHashCodeAndHashPair.first
        val elementToRemove =  tmpHashCodeAndHashPair.second

        val tmpSet = javaHashCodeToGtvSet_Map[javaHashCode]!!

        val removedElem = tmpSet.prune(elementToRemove)
        if (removedElem != null) {
            val nrOfBytes = removedElem.merkleHashSummary.nrOfBytes
            totalSizeInBytes -= nrOfBytes
            if (logger.isDebugEnabled) {
                logger.debug("Cache item pruned! timestamp: $timestampToRemove : $nrOfBytes bytes ")
            }
        } else {
            logger.warn("Why didn't we find the element in the set? hashCode: $javaHashCode timestamp: $timestampToRemove ")
        }
        if (tmpSet.internalSet.isEmpty()) {
            javaHashCodeToGtvSet_Map.remove(javaHashCode)
        }
    }


}