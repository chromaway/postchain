package net.postchain.gtv.merkle.cache

import net.postchain.gtv.merkle.*
import net.postchain.gtv.merkleHash
import org.junit.Assert
import org.junit.Test

class MixArrayDictToGtvBinaryTreeWithCacheTest {

    private val factory = GtvBinaryTreeFactory()

    fun checkStats(localHits: Int, globalHits: Int, misses: Int, memoization: GtvMerkleHashMemoization) {
        println("Cache content! local hits: ${memoization.localCacheHits}, global hits: ${memoization.globalCacheHits}, cacheMisses: ${memoization.cacheMisses}")
        Assert.assertEquals(localHits.toLong(), memoization.localCacheHits)
        Assert.assertEquals(globalHits.toLong(), memoization.globalCacheHits)
        Assert.assertEquals(misses.toLong(), memoization.cacheMisses)
    }

    @Test
    fun calculate_inner_array_and_then_dictWithArray() {
        if (!GtvMerkleHashCache.enabled) {
            return
        }
        val calculator = MerkleHashCalculatorDummy()
        val memoization = calculator.memoization as GtvMerkleHashMemoization

        // -------------------  Calculate inner array of 4 (just to the primitives into cache) ---------------------
        val gtvInnerArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf4()
        val root1 = gtvInnerArr.merkleHash(calculator)

        val maxTotalLookups = 4  // The outer GtvArray is not counted
        val cacheMisses1 = maxTotalLookups
        checkStats(0, 0, cacheMisses1, memoization)

        val gtvDict = MixArrayDictToGtvBinaryTreeHelper.buildGtvDictWithSubArray4()
        val root2 = gtvDict.merkleHash(calculator)

        val maxTotalLookups2 = 5
        val potentialHits = 4
        val realTotalHits = potentialHits // We expect success
        val newMisses = (maxTotalLookups2 - realTotalHits) // 5 - 4 = 1

        val cacheLocalHist2 = 0 // We are not re-using the instances
        val cacheGlobalHits2 = realTotalHits - cacheLocalHist2 // 4 - 0
        val cacheMisses2 = cacheMisses1 + newMisses  // 5 + 1
        checkStats(cacheLocalHist2, cacheGlobalHits2, cacheMisses2, memoization) // 0, 4, 6
    }
}