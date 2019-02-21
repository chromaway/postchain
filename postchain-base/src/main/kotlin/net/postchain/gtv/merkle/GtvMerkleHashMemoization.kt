package net.postchain.gtv.merkle

import mu.KLogging
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.base.merkle.MerkleHashMemoization
import net.postchain.base.merkle.proof.MerkleHashSummary
import net.postchain.gtv.Gtv

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

    fun findOrCalculateMerkleHash(gtvSrc: Gtv, calculator: MerkleHashCalculator<Gtv>): MerkleHashSummary? {
        return gtvMerkleHashMemoization.findMerkleHash(gtvSrc)
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
class GtvMerkleHashMemoization(val TRY_PRUNE_AFTER_THIS_MANY_LOOKUPS: Int ,val MAX_CACHE_SIZE_BYTES: Int): MerkleHashMemoization<Gtv>() {

    companion object : KLogging()

    private val javaHashCodeToGtvSet_Map = HashMap<Int, MerkleHashSetWithSameGtvJavaHashCode>()

    // Some statistics (Not private due to testing)
    var localCacheHits = 0L // How many times the local cache was successful
    var globalCacheHits = 0L // How many times the global cache was successful
    var cacheMisses = 0L // How many times no cached value was found

    private var totalSizeInBytes = 0 // Keeps track of how much space the cache consumes. This is not exact, but gives worst case!

    /**
     * Use this method so cannot modify the value from outside (since Kotlin does a copy)
     */
    fun getTotalSizeInBytes() = totalSizeInBytes

    /**
     * The cache will consume more than just the memory of the [Gtv] object, i.e. the "overhead"
     */
    private fun increaseTotalMem(increaseBytes: Int) {
        val addedNrOfBytes = (increaseBytes + MerkleHashSetWithSameGtvJavaHashCode.OVERHEAD_SIZE_BYTES)
        if (logger.isDebugEnabled) {
            logger.debug("add to cache: $addedNrOfBytes, (= bytes: ${increaseBytes} + overhead: ${MerkleHashSetWithSameGtvJavaHashCode.OVERHEAD_SIZE_BYTES}) ")
        }
        totalSizeInBytes += addedNrOfBytes
    }

    /**
     * The cache will consume more than just the memory of the [Gtv] object, i.e. the "overhead"
     */
    private fun decreaseTotalMem(decreaseBytes: Int, timestamp: Long) {
        val nrOfBytesToDecrease = decreaseBytes +  MerkleHashSetWithSameGtvJavaHashCode.OVERHEAD_SIZE_BYTES
        if (logger.isDebugEnabled) {
            logger.debug("Cache item pruned! timestamp: $timestamp : $nrOfBytesToDecrease bytes ")
        }
        totalSizeInBytes -= nrOfBytesToDecrease
    }

    /**
     * Will look in the local cache of the [Gtv] instance before looking in the global cache.
     *
     * @param src the [Gtv] structure we need to get a merkle hash out of
     * @return the merkle root hash we for the given src
     */
    override fun findMerkleHash(src: Gtv): MerkleHashSummary? {
        var retMerkleHashSummary: MerkleHashSummary? = null

        val localCachedVal = src.getCachedMerkleHash()
        if (localCachedVal != null) {
            // Found in Local cache
            synchronized(this) {
                localCacheHits++
            }
            retMerkleHashSummary = localCachedVal  // Return immediately (no pruning etc.)
        } else {
            // Nope, look in Global cache
            val gtvSrcHashCode = src.hashCode()
            retMerkleHashSummary = lookForSrcInInternalMap(src, gtvSrcHashCode)
            if (retMerkleHashSummary != null) {
                src.setCachedMerkleHash(retMerkleHashSummary)

                synchronized(this) {
                    globalCacheHits++
                }
            } else {
                synchronized(this) {
                    cacheMisses++
                }
            }
        }

        maybePrune()

        return retMerkleHashSummary
    }

    private fun lookForSrcInInternalMap(src: Gtv, gtvSrcHashCode: Int): MerkleHashSummary? {
        val foundGtvSet = javaHashCodeToGtvSet_Map[gtvSrcHashCode]
        return if (foundGtvSet != null) {
            foundGtvSet.findFromGtv(src)
        } else {
            null
        }
    }

    /**
     * Will add the [MerkleHashSummary] to the cache (both local cache and global cache)
     * Note: We assume that the "src" should not exist in any cache (or else it would have been found already).
     *
     * @param src is the [Gtv] source we should map the summary to
     * @param newSummary is the value we need to cache
     */
    @Synchronized
    override fun add(src: Gtv, newSummary: MerkleHashSummary) {
        // Add 2 Local
        if (src.getCachedMerkleHash() == null) {
            src.setCachedMerkleHash(newSummary)
        }else {
            logger.warn("Why do we need to add to cache when local cache has a value? ")
        }

        // Add 2 Global
        val gtvSrcHashCode = src.hashCode()

        val foundGtvSet = javaHashCodeToGtvSet_Map[gtvSrcHashCode]
        if (foundGtvSet != null) {
            val summaryAlreadyAdded = foundGtvSet.findFromGtv(src)
            if (summaryAlreadyAdded != null) {
                logger.warn("Why do we need to add to cache when global cache has a value? ")
            }
        }

        addToChache(src, newSummary, foundGtvSet, gtvSrcHashCode)
    }

    /**
     * Note: this hash to be synchronized since we are updating the cache itself
     */
    @Synchronized
    private fun addToChache(gtvSrc: Gtv, calculatedMerkleHashSummary: MerkleHashSummary, foundGtvSet: MerkleHashSetWithSameGtvJavaHashCode?, javaHashCode: Int) {
        if (foundGtvSet != null) {
            foundGtvSet.addToSet(gtvSrc, calculatedMerkleHashSummary)
        } else {
            val newSet = MerkleHashSetWithSameGtvJavaHashCode.build(gtvSrc, calculatedMerkleHashSummary)
            javaHashCodeToGtvSet_Map[javaHashCode] = newSet
        }
        increaseTotalMem(calculatedMerkleHashSummary.nrOfBytes)

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
        var didPrune = false
        try {
            val allInteraction = localCacheHits + globalCacheHits + cacheMisses
            if (allInteraction.rem(TRY_PRUNE_AFTER_THIS_MANY_LOOKUPS) == 0L) {
                logger.debug("Time to check if cache is too big.")

                // Let's see if we should prune
                if (totalSizeInBytes > MAX_CACHE_SIZE_BYTES) {
                    // 0. Begin
                    logger.info("----------------------------------------------------------------------------------")
                    logger.info("Begin pruning GtvMerkleHashMemoization, size: ${totalSizeInBytes} bytes (interactions: $allInteraction)")
                    logger.info("----------------------------------------------------------------------------------")
                    val begin = System.currentTimeMillis()
                    didPrune = true // Do this now, we might crash later

                    // 1. Build a structure where timestamp is key
                    // (We use a set inside the hash map for the off chance of two cached items get the same timestamp)
                    val timeStampToHashMap = HashMap<Long, MutableSet<Pair<Int, CacheElement>>>()
                    for (key in javaHashCodeToGtvSet_Map.keys) {
                        val tmpSet = javaHashCodeToGtvSet_Map[key]!!
                        for (element in tmpSet.internalSet) {
                            val currentTimestamp = element.age
                            val foundSet = timeStampToHashMap[currentTimestamp]
                            if (foundSet != null) {
                                // Add a pair to set
                                foundSet.add(Pair(key, element))
                                if (logger.isDebugEnabled) {
                                    logger.debug("${foundSet.size} objects (java hash code=$key) with the same creation timestamp ($currentTimestamp) ? Unusual! ")
                                }
                            } else {
                                // Create a set and add the element
                                val newSet = mutableSetOf(Pair(key, element))
                                timeStampToHashMap[currentTimestamp] = newSet
                            }
                        }
                    }

                    // 2. Remove oldest timestamps first
                    val allTimestamps: List<Long> = timeStampToHashMap.keys.sorted().asReversed()

                    val idealSizeInBytes = MAX_CACHE_SIZE_BYTES / 2 // Our goal is to make the cache half-empty so we don't have to do this often
                    var pruned = 0
                    while (totalSizeInBytes > idealSizeInBytes) {
                        val timestampToRemove = allTimestamps[pruned]
                        // Remove ALL objects with this timestamp
                        val setToRemove = timeStampToHashMap[timestampToRemove]
                        for (pair in setToRemove!!) {
                            removeFromSet(timestampToRemove, pair)
                        }
                        pruned++
                    }

                    // 3. Done. Print and erase stats
                    val hitrateLocal: Double = 100.0 * (localCacheHits.toDouble() / allInteraction)
                    val hitrateGlobal: Double = 100.0 * ((localCacheHits.toDouble() + globalCacheHits.toDouble()) / allInteraction)
                    val localHitrateStr = "%.2f".format(hitrateLocal)
                    val globalHitrateStr = "%.2f".format(hitrateGlobal)
                    val end = System.currentTimeMillis()
                    val duration = end - begin
                    logger.info("----------------------------------------------------------------------------------")
                    logger.info("Cache pruned successfully down to size: $totalSizeInBytes (objects pruned: $pruned) in $duration ms. Local Hitrate = $localHitrateStr% Global Hitrate = $globalHitrateStr% (local hits: $localCacheHits, global hits: $globalCacheHits, misses: $cacheMisses)")
                    logger.info("----------------------------------------------------------------------------------")
                    localCacheHits = 0
                    globalCacheHits = 0
                    cacheMisses = 0
                }
            }
        } catch (e: Exception) {
            // Shouldn't let the failed pruning ruin everything, let's log it and move on
            logger.error ("Pruning failed with error: ", e)
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
    private fun removeFromSet(timestampToRemove: Long,tmpHashCodeAndHashPair: Pair<Int, CacheElement>)  {
        val javaHashCode = tmpHashCodeAndHashPair.first
        val elementToRemove =  tmpHashCodeAndHashPair.second

        val tmpSet = javaHashCodeToGtvSet_Map[javaHashCode]!!

        val removedElem = tmpSet.prune(elementToRemove)
        if (removedElem != null) {
            decreaseTotalMem(removedElem.merkleHashSummary.nrOfBytes, timestampToRemove)
        } else {
            logger.warn("Why didn't we find the element in the set? hashCode: $javaHashCode timestamp: $timestampToRemove ")
        }
        if (tmpSet.internalSet.isEmpty()) {
            javaHashCodeToGtvSet_Map.remove(javaHashCode)
        }
    }

}