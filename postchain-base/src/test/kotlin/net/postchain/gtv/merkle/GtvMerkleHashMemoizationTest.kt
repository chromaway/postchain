package net.postchain.gtv.merkle

import net.postchain.base.merkle.Hash
import net.postchain.base.merkle.proof.MerkleHashSummary
import org.junit.Assert
import org.junit.Test

/**
 * Since the cache works with mutable state, it is better to test this usince scenario tests: i.e. we do many
 * small checks in one big test, where we slowly building up the state of the cache.
 */
class GtvMerkleHashMemoizationTest {

    val lookupsBeforePrune = 3
    val maxSizeBytes = 200
    val memoization = GtvMerkleHashMemoization(lookupsBeforePrune, maxSizeBytes)
    val overheadSize = MerkleHashSetWithSameGtvJavaHashCode.OVERHEAD_SIZE_BYTES

    val dummyHash: Hash = ByteArray(32)

    private fun buildDummyHash(seed: Byte): Hash {
        val clone = dummyHash.clone()
        clone[0] = seed
        return clone
    }

    fun checkStats(localHits: Int, globalHits: Int, misses: Int) {
        Assert.assertEquals(localHits.toLong(), memoization.localCacheHits)
        Assert.assertEquals(globalHits.toLong(), memoization.globalCacheHits)
        Assert.assertEquals(misses.toLong(), memoization.cacheMisses)
    }

    fun checkSize(expectedSize: Int) {
        //println("size: ${memoization.getTotalSizeInBytes()}")
        Assert.assertEquals(expectedSize, memoization.getTotalSizeInBytes())
    }

    @Test
    fun basic_operations_and_pruning_scenario_test() {
        // ------------ Before we start -----------
        checkStats(0,0,0)
        println("size: ${memoization.getTotalSizeInBytes()}")
        Assert.assertEquals(0, memoization.getTotalSizeInBytes())

        // ------------ Add a size 1 array to the cache -----------
        val sizeOf1Longs = 1 * 8
        val expectedSizeOf1Longs = sizeOf1Longs + overheadSize

        val gtvArr1 = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf1()

        val hashSummary1 = memoization.findMerkleHash(gtvArr1) // Interractions = 1
        Assert.assertNull(hashSummary1) // Should not be found
        checkStats(0,0,1)
        checkSize(0)

        // Not found, so realistically the user will add the summary to the cache here
        val dummyHashSummary1 = MerkleHashSummary(buildDummyHash(1.toByte()) , sizeOf1Longs)
        memoization.add(gtvArr1, dummyHashSummary1)

        checkStats(0,0,1)
        checkSize(expectedSizeOf1Longs)

        // ------------ Size= 1 array should be found -----------
        val hashSummary1Found = memoization.findMerkleHash(gtvArr1) // Interractions = 2
        Assert.assertNotNull(hashSummary1Found) // Should be found
        checkStats(1,0,1)
        checkSize(expectedSizeOf1Longs)

        Thread.sleep(1)
        // ------------ Add a size 7 array to the cache -----------
        val sizeOf7Longs = 7 * 8
        val expectedSizeOf7Longs = sizeOf7Longs + overheadSize
        val gtvArr7 = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf7()

        val hashSummary7 = memoization.findMerkleHash(gtvArr7) // Interractions = 3
        Assert.assertNull(hashSummary7) // Should not be found
        checkStats(1,0,2)
        checkSize(expectedSizeOf1Longs)

        // Not found, so realistically the user will add the summary to the cache here
        val dummyHashSummary7 = MerkleHashSummary(buildDummyHash(7.toByte()), sizeOf7Longs)
        memoization.add(gtvArr7, dummyHashSummary7)

        checkStats(1,0,2)
        checkSize((expectedSizeOf7Longs + expectedSizeOf1Longs))

        // ------------ Add a size 9 array to the cache -----------
        val sizeOf9Longs = 9 * 8
        val expectedSizeOf9Longs = sizeOf9Longs + overheadSize
        val gtvArr9 = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf9()

        val hashSummary9 = memoization.findMerkleHash(gtvArr9) // Interractions = 4
        Assert.assertNull(hashSummary9) // Should not be found
        checkStats(1,0,3)
        checkSize((expectedSizeOf7Longs + expectedSizeOf1Longs))

        // Not found, so the summary should be added to the cache here
        val dummyHashSummary9 = MerkleHashSummary(buildDummyHash(9.toByte()), sizeOf9Longs)
        memoization.add(gtvArr9, dummyHashSummary9)

        checkStats(1,0,3)
        checkSize((expectedSizeOf9Longs + expectedSizeOf7Longs + expectedSizeOf1Longs))

        // ------------ Size= 9 array should be found -----------
        val hashSummary9Found = memoization.findMerkleHash(gtvArr9) // Interractions = 5
        Assert.assertNotNull(hashSummary9Found) // Should be found
        checkStats(2,0,3)
        checkSize((expectedSizeOf9Longs + expectedSizeOf7Longs + expectedSizeOf1Longs))

        // ------------ Size= 7 array should still be found -----------
        val hashSummary7Found_firstTry = memoization.findMerkleHash(gtvArr7) // Interractions = 6
        Assert.assertNotNull(hashSummary7Found_firstTry) // Should be found

        // ------------ Now the prune will have run, because we have reached the limit. -----------
        checkStats(0,0,0)
        checkSize(expectedSizeOf1Longs)

        // ------------ Size= 1 array should still be found -----------
        // The reason array size 1 was not pruned is that it has a small size and was added first
        val hashSummary1Found_notPruned = memoization.findMerkleHash(gtvArr1) // Interractions = 7 (-> 1)
        Assert.assertNotNull(hashSummary1Found_notPruned) // Should be found
        checkStats(1,0,0)
        checkSize(expectedSizeOf1Longs)
    }

}