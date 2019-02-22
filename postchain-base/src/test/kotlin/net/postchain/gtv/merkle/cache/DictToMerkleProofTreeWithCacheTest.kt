package net.postchain.gtv.merkle.cache

import net.postchain.common.toHex
import net.postchain.gtv.*
import net.postchain.gtv.merkle.*
import net.postchain.gtv.merkle.DictToGtvBinaryTreeHelper.expectedMerkleRoot4
import net.postchain.gtv.merkle.proof.merkleHash
import org.junit.Assert
import org.junit.Test

/**
 * Aim of these tests are to test that the cache is used properly when doing proofs on Dict structures.
 */
class DictToMerkleProofTreeWithCacheTest {

    val calculator = MerkleHashCalculatorDummy()

    fun checkStats(localHits: Int, globalHits: Int, misses: Int, memoization: GtvMerkleHashMemoization) {
        //println("Cache content! local hits: ${memoization.localCacheHits}, global hits: ${memoization.globalCacheHits}, cacheMisses: ${memoization.cacheMisses}")
        Assert.assertEquals(localHits.toLong(), memoization.localCacheHits)
        Assert.assertEquals(globalHits.toLong(), memoization.globalCacheHits)
        Assert.assertEquals(misses.toLong(), memoization.cacheMisses)
    }

    @Test
    fun calculate_same_4dict_proof_two_times() {
        val memoization = calculator.memoization as GtvMerkleHashMemoization
        val gtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf4()

        // ------------------- First part, path to "four" ---------------------
        val path: Array<Any> = arrayOf("four")
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPathSet = GtvPathSet(setOf(gtvPath))

        val proof1 = gtvDict.generateProof(gtvPathSet, calculator)

        val maxTotalLookups = 8
        val notTryingToFindNr4 = 1
        val cacheMisses1 = maxTotalLookups - notTryingToFindNr4  // one less since "4" will not be cached, since it's not calculated
        checkStats(0,0, cacheMisses1, memoization)

        // ------------------- Second time, same stuff ---------------------

        val proof2 = gtvDict.generateProof(gtvPathSet, calculator)

        val realTotalHits = maxTotalLookups - notTryingToFindNr4 // Cannot get more than 7, since the proven "4" will not be looked for
        val maximumPotentialLocalCacheHits = 4  // The keys in the dict will never be found in local GTV instances

        val cacheLocalHist2 = maximumPotentialLocalCacheHits - notTryingToFindNr4
        val cacheGlobalHits2 = realTotalHits - cacheLocalHist2
        val cacheMisses2 = cacheMisses1 // No new misses
        checkStats(cacheLocalHist2, cacheGlobalHits2, cacheMisses2, memoization) // 3, 4, 7

        // ----------------- Calculate the hash using cached values ---------------------

        // Make sure the cached values give the correct root
        val root = proof2.merkleHash(calculator)
        Assert.assertEquals(expectedMerkleRoot4 , root.toHex())

        val cacheLocalHist3 = cacheLocalHist2
        val cacheGlobalHits3 = cacheGlobalHits2
        val cacheMisses3 = cacheMisses2 + 1 // We tried to get the "4" from the cache, but it was never calculated

        checkStats(cacheLocalHist3, cacheGlobalHits3, cacheMisses3, memoization) // 3, 4, 8
    }

    @Test
    fun calculate_4dict_proof_with_different_paths() {
        val memoization = calculator.memoization as GtvMerkleHashMemoization
        val gtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf4()

        // ------------------- First part, path to "four" ---------------------
        val path: Array<Any> = arrayOf("four")
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPathSet = GtvPathSet(setOf(gtvPath))

        val proof1 = gtvDict.generateProof(gtvPathSet, calculator)

        val maxTotalLookups = 8
        val notTryingToFindNr4 = 1
        val cacheMisses1 = maxTotalLookups - notTryingToFindNr4  // one less since "4" will not be cached, since it's not calculated
        checkStats(0,0, cacheMisses1, memoization)

        // ------------------- Second part, path to "two" ---------------------
        val path2: Array<Any> = arrayOf("two")
        val gtvPath2: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path2)
        val gtvPathSet2 = GtvPathSet(setOf(gtvPath2))

        val proof2 = gtvDict.generateProof(gtvPathSet2, calculator)

        val notTryingToFindNr2 = 1 // Not even trying to find "2"
        val missDueToLackingNr4 = 1  // We will try to find "4" but it has never been cached
        val realTotalHits = maxTotalLookups - notTryingToFindNr2 - missDueToLackingNr4 // Cannot get more than 6

        val maximumPotentialLocalCacheHits = 4  // The keys in the dict will never be found in local GTV instances

        val cacheLocalHist2 = maximumPotentialLocalCacheHits - (missDueToLackingNr4 + notTryingToFindNr2) // "2" is in the local cache, but we are not looking for it
        val cacheGlobalHits2 = realTotalHits - cacheLocalHist2
        val cacheMisses2 = cacheMisses1 + missDueToLackingNr4
        checkStats(cacheLocalHist2,cacheGlobalHits2, cacheMisses2, memoization) // 2, 4, 8

        // ----------------- Calculate the hash using cached values ---------------------

        // Make sure the cached values give the correct root
        val root = proof2.merkleHash(calculator)
        Assert.assertEquals(expectedMerkleRoot4 , root.toHex())

        val cacheLocalHist3 = cacheLocalHist2 + 1 // We tried to get the "2" from the cache, and it was stored previously
        val cacheGlobalHits3 = cacheGlobalHits2
        val cacheMisses3 = cacheMisses2

        checkStats(cacheLocalHist3, cacheGlobalHits3, cacheMisses3, memoization) // 2, 4, 8
    }


}