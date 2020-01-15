package net.postchain.gtv.merkle

import mu.KLogging
import net.postchain.base.merkle.MerkleHashMemoization
import net.postchain.base.merkle.proof.MerkleHashSummary
import net.postchain.common.toHex
import net.postchain.gtv.*
import kotlin.math.abs
import kotlin.random.Random

/**
 * To improve performance, we will cache the hash of a given [GtvPrimitive] object.
 * (Well look in the cache, and only if not found we'll calculate the hash)
 *
 * The reason we don't cache [GtvArray] is that most transactions are different, so there is not much to gain.
 *
 * Note1: that there is a high risk for collisions on Java's hashCode(), so that is why we have to store a set of all
 * [Gtv]s with this hash code. This might be a threat to memory, so we need a way to prune the cache.
 *
 *
 */
object GtvMerkleHashCache {

    val enabled : Boolean = false
    val defaultSizeOf1MB = 1000000 // TODO: Make this configurable
    val defaultNumberOfLookupsBeforePrune = 100 // Not that important, no need to configure

    val gtvMerkleHashMemoization = GtvMerkleHashMemoization(defaultNumberOfLookupsBeforePrune, defaultSizeOf1MB)
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
class GtvMerkleHashMemoization(val TRY_PRUNE_AFTER_THIS_MANY_LOOKUPS: Int ,val MAX_CACHE_SIZE_BYTES: Int, val EXTRA_STATS: Boolean = false): MerkleHashMemoization<Gtv>() {

    companion object : KLogging()

    private val javaHashCodeToGtvSet_Map = HashMap<Int, MerkleHashSetWithSameGtvJavaHashCode>()
    private val rndNr: Int = abs(Random.nextInt() / 1000000).toInt()

    // Some statistics (Not private due to testing)
    var localCacheHits = 0L // How many times the local cache was successful
    var globalCacheHits = 0L // How many times the global cache was successful
    var cacheMisses = 0L // How many times no cached value was found
    private var totalSizeInBytes = 0 // Keeps track of how much space the cache consumes. This is not exact, but gives worst case!

    // -----------------
    // Type specific maps (only used during EXTRA_STATS extra debugging)
    // -----------------
    var elementTypeHashMap = HashMap<String, Int>() // Describes how many objects of a specific type we have added since last pruning.

    var searchedIntegerHashMap = HashMap<Int, Int>() // Number of times a specific GTV int was searched for
    var searchStringsHash = HashMap<String, Int>()  // ... GTV string ..
    var searchByteArrayHash = HashMap<String, Int>() // ... GTV ByteArray ..

    var hitsIntegerHashMap = HashMap<Int, Int>() // Number of times a specific GTV int was hit
    var hitsStringsHash = HashMap<String, Int>()  // ... GTV string ..
    var hitsByteArrayHash = HashMap<String, Int>() // ... GTV ByteArray ..


    /**
     * Use this method so cannot modify the value from outside (since Kotlin does a copy)
     */
    fun getTotalSizeInBytes() = totalSizeInBytes

    /**
     * The cache will consume more than just the memory of the [Gtv] object, i.e. the "overhead"
     */
    private fun increaseTotalMem(increaseBytes: Int) {
        val addedNrOfBytes = (increaseBytes + MerkleHashSetWithSameGtvJavaHashCode.OVERHEAD_SIZE_BYTES)
        if (logger.isTraceEnabled) {
            logger.trace("add to cache: $addedNrOfBytes, (= bytes: ${increaseBytes} + overhead: ${MerkleHashSetWithSameGtvJavaHashCode.OVERHEAD_SIZE_BYTES}) ")
        }
        totalSizeInBytes += addedNrOfBytes
    }

    /**
     * The cache will consume more than just the memory of the [Gtv] object, i.e. the "overhead"
     */
    private fun decreaseTotalMem(decreaseBytes: Int, timestamp: Long) {
        val nrOfBytesToDecrease = decreaseBytes +  MerkleHashSetWithSameGtvJavaHashCode.OVERHEAD_SIZE_BYTES
        if (logger.isTraceEnabled) {
            logger.trace("Cache item pruned! timestamp: $timestamp : $nrOfBytesToDecrease bytes ")
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
        if (!GtvMerkleHashCache.enabled) return null

        var retMerkleHashSummary: MerkleHashSummary? = null

        if (!(src is GtvPrimitive)) {
            logger.warn("Who is searching for ${src.type} in the cache?")
            return null
        }

        if (EXTRA_STATS) {
            debugSearches(src)
        }

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
                    if (EXTRA_STATS) {
                        debugHits(src)
                    }
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
        if (!GtvMerkleHashCache.enabled) return

        if (!(src is GtvPrimitive)) {
            logger.warn("Who is adding a ${src.type} to the cache?")
            return
        }

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
                logger.warn("$rndNr, Why do we need to add to cache when global cache has a value? hashCode: $gtvSrcHashCode , gtv: ${src.toString()}")
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

        if (EXTRA_STATS) {
            val type = gtvSrc.type.name
            val nr = elementTypeHashMap[type] ?: 0
            elementTypeHashMap[type] = 1 + nr
        }
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
                //logger.debug("Time to check if cache is too big. size: $totalSizeInBytes Bytes")

                // Let's see if we should prune
                if (totalSizeInBytes > MAX_CACHE_SIZE_BYTES) {
                    // 0. Begin
                    logger.debug("----------------------------------------------------------------------------------")
                    logger.debug("$rndNr, Begin pruning GtvMerkleHashMemoization, size: ${totalSizeInBytes} bytes (interactions: $allInteraction)")
                    logger.debug("----------------------------------------------------------------------------------")
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
                                //if (logger.isDebugEnabled) {
                                //    logger.debug("${foundSet.size} objects (java hash code=$key) with the same creation timestamp ($currentTimestamp) ? Unusual! ")
                                //}
                            } else {
                                // Create a set and add the element
                                val newSet = mutableSetOf(Pair(key, element))
                                timeStampToHashMap[currentTimestamp] = newSet
                            }
                        }
                    }

                    var nrOfStamps = 0
                    var sumOfValues = 0
                    for (timestamp in timeStampToHashMap.keys) {
                        nrOfStamps++
                        val sum = timeStampToHashMap[timestamp]!!.size
                        val list = timeStampToHashMap[timestamp]!!
                        sumOfValues += sum

                        if (EXTRA_STATS) {
                            if (sum > 10) {
                                logger.trace("timestamp: $timestamp, sum: $sum  ")
                            }
                        }
                    }

                    if (EXTRA_STATS) {
                        logger.debug("--------Types----------  ")
                        for (type in elementTypeHashMap.keys) {
                            logger.debug("type: $type, nr: ${elementTypeHashMap[type]}")
                        }
                        searchStats()
                        hitStats()
                    }

                    logger.debug("------------------  ")
                    logger.debug("$rndNr, Total unique timestamps: $nrOfStamps, sum: $sumOfValues, avg objects stored per timestamp: ${sumOfValues.toDouble()/nrOfStamps} ")
                    logger.debug("------------------  ")

                    // 2. Remove oldest timestamps first
                    val allTimestamps: List<Long> = timeStampToHashMap.keys.sorted().asReversed()

                    val idealSizeInBytes = MAX_CACHE_SIZE_BYTES / 2 // Our goal is to make the cache half-empty so we don't have to do this often
                    var pruned = 0
                    while (totalSizeInBytes > idealSizeInBytes && pruned < allTimestamps.size) {
                        val timestampToRemove = allTimestamps[pruned]
                        // Remove ALL objects with this timestamp
                        val setToRemove = timeStampToHashMap[timestampToRemove]
                        for (pair in setToRemove!!) {
                            removeFromSet(timestampToRemove, pair)
                        }
                        pruned++
                    }

                    // 3. Done. Print stats
                    val hitrateLocal: Double = 100.0 * (localCacheHits.toDouble() / allInteraction)
                    val hitrateGlobal: Double = 100.0 * ((localCacheHits.toDouble() + globalCacheHits.toDouble()) / allInteraction)
                    val localHitrateStr = "%.2f".format(hitrateLocal)
                    val globalHitrateStr = "%.2f".format(hitrateGlobal)
                    val end = System.currentTimeMillis()
                    val duration = end - begin
                    logger.debug("----------------------------------------------------------------------------------")
                    logger.debug("Cache pruned successfully down to size: $totalSizeInBytes (objects pruned: $pruned) in $duration ms. Local Hitrate = $localHitrateStr% Global Hitrate = $globalHitrateStr% (local hits: $localCacheHits, global hits: $globalCacheHits, misses: $cacheMisses)")
                    logger.debug("----------------------------------------------------------------------------------")

                    // 4. Erase stats
                    localCacheHits = 0
                    globalCacheHits = 0
                    cacheMisses = 0
                    if (EXTRA_STATS) {
                        resetExtraStats()
                    }
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

    private fun searchStats() {
        logger.trace("------GtvIntegers--------  ")
        var intCount = 0
        for (int in searchedIntegerHashMap.keys.sorted()) {
            val count = searchedIntegerHashMap[int]!!
            intCount += count
            if (count > 10) {
                logger.trace("search int: $int, nr: ${count}")
            }
        }

        logger.debug("unique search integers: ${searchedIntegerHashMap.keys.size}, total searches: $intCount  ")
        logger.trace("------------------  ")

        logger.trace("------GtvStrings--------  ")
        var strCount = 0
        for (str in searchStringsHash.keys.sorted()) {
            val count = searchStringsHash[str]!!
            strCount += count
            if (count > 10) {
                logger.trace("search string: $str, nr: ${count}")
            }
        }

        logger.debug("unique search strings: ${searchStringsHash.keys.size}, total searches: $strCount  ")
        logger.trace("------------------  ")

        logger.trace("------GtvByteArrays--------  ")
        var baCount = 0
        for (str in searchByteArrayHash.keys.sorted()) {
            val count =searchByteArrayHash[str]!!
            baCount += count
            if (count > 10) {
                logger.trace("search byte array: $str, nr: $count")
            }

        }
        logger.debug(" unique search byte array: ${searchByteArrayHash.keys.size}, total searches: $baCount  ")
        logger.trace("------------------  ")


    }

    private fun hitStats() {
        logger.trace("------GtvIntegers--------  ")
        var intCount = 0
        for (int in hitsIntegerHashMap.keys.sorted()) {
            val count = hitsIntegerHashMap[int]!!
            intCount += count
            if (count > 10) {
                logger.trace("hit int: $int, nr: ${count}")
            }
        }

        logger.debug("unique hit integers: ${hitsIntegerHashMap.keys.size}, total hits: $intCount  ")
        logger.trace("------------------  ")

        logger.trace("------GtvStrings--------  ")
        var strCount = 0
        for (str in hitsStringsHash.keys.sorted()) {
            val count = hitsStringsHash[str]!!
            strCount += count
            if (count > 10) {
                logger.trace("search string: $str, nr: ${count}")
            }
        }

        logger.debug("unique hit strings: ${hitsStringsHash.keys.size}, total hits: $strCount  ")
        logger.trace("------------------  ")

        logger.trace("------GtvByteArrays--------  ")
        var baCount = 0
        for (str in hitsByteArrayHash.keys.sorted()) {
            val count =hitsByteArrayHash[str]!!
            baCount += count
            if (count > 10) {
                logger.trace("search byte array: $str, nr: $count")
            }

        }
        logger.debug(" unique hit byte array: ${hitsByteArrayHash.keys.size}, total hits: $baCount  ")
        logger.trace("------------------  ")
    }



    private fun debugSearches(src: Gtv) {
        synchronized(this) {
            when (src) {
                is GtvInteger -> {
                    val content = src.integer.intValueExact()
                    val nr = searchedIntegerHashMap[content] ?: 0
                    searchedIntegerHashMap[content] = nr + 1
                }
                is GtvString -> {
                    val nr = searchStringsHash[src.string] ?: 0
                    searchStringsHash[src.string] = nr + 1
                }
                is GtvByteArray -> {
                    val baStr = src.bytearray.toHex()
                    val nr = searchByteArrayHash[baStr] ?: 0
                    searchByteArrayHash[baStr] = 1 + nr
                }
                else -> logger.debug("What is this? ${src.type}")
            }
        }
    }

    private fun debugHits(src: Gtv) {
        when (src) {
            is GtvInteger -> {
                val content = src.integer.intValueExact()
                val nr = hitsIntegerHashMap[content] ?: 0
                hitsIntegerHashMap[content] = nr + 1
            }
            is GtvString -> {
                val nr = hitsStringsHash[src.string] ?: 0
                hitsStringsHash[src.string] = nr + 1
            }
            is GtvByteArray -> {
                val baStr = src.bytearray.toHex()
                val nr = hitsByteArrayHash[baStr] ?: 0
                hitsByteArrayHash[baStr] = 1 + nr
            }
            else -> logger.debug("What is this? ${src.type}")
        }
    }

    private fun resetExtraStats() {
        elementTypeHashMap = HashMap<String, Int>()

        searchedIntegerHashMap = HashMap<Int, Int>()
        searchStringsHash = HashMap<String, Int>()
        searchByteArrayHash = HashMap<String, Int>()

        hitsIntegerHashMap = HashMap<Int, Int>()
        hitsStringsHash = HashMap<String, Int>()
        hitsByteArrayHash = HashMap<String, Int>()
    }

}