package net.postchain.gtv.merkle.cache

import net.postchain.gtv.*
import net.postchain.gtv.merkle.ArrayToGtvBinaryTreeHelper
import net.postchain.gtv.merkle.GtvMerkleHashMemoization
import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import org.junit.Assert
import org.junit.Test

/**
 * The aim of these tests is to check if cache is used correctly when calculating merkle roots from arrays.
 */
class ArrayToMerkleRootWithCacheTest {


    fun checkStats(localHits: Int, globalHits: Int, misses: Int, memoization: GtvMerkleHashMemoization) {
        //println("Cache content! local hits: ${memoization.localCacheHits}, global hits: ${memoization.globalCacheHits}, cacheMisses: ${memoization.cacheMisses}")
        Assert.assertEquals(localHits.toLong(), memoization.localCacheHits)
        Assert.assertEquals(globalHits.toLong(), memoization.globalCacheHits)
        Assert.assertEquals(misses.toLong(), memoization.cacheMisses)
    }

    /**
     * The idea with this test is that the entire inner array should be cached and found in cache when we calculate the
     * array in array (7 with inner 3).
     */
    @Test
    fun calculate_inner_array_and_then_arrayInArray() {
        val calculator = MerkleHashCalculatorDummy()
        val memoization = calculator.memoization as GtvMerkleHashMemoization

        // -------------------  Calculate inner array of 3 (just to get it into cache) ---------------------
        val gtvInnerArr = ArrayToGtvBinaryTreeHelper.buildGtvArrInnerOf3()
        val root1 = gtvInnerArr.merkleHash(calculator)

        val maxTotalLookups = 3  // The outer GtvArray is not counted
        val cacheMisses1 = maxTotalLookups
        checkStats(0, 0, cacheMisses1, memoization)

        // ------------------- Do array in array ---------------------
        val gtvArrInArr = ArrayToGtvBinaryTreeHelper.buildGtvArrOf7WithInner3()
        val root2 = gtvArrInArr.merkleHash(calculator)

                    /*  "                               +                                 \n" +
                        "                              / \\                               \n" +
                        "                             /   \\                              \n" +
                        "                            /     \\                             \n" +
                        "                           /       \\                            \n" +
                        "                          /         \\                           \n" +
                        "                         /           \\                          \n" +
                        "                        /             \\                         \n" +
                        "                       /               \\                        \n" +
                        "                      /                 \\                       \n" +
                        "                     /                   \\                      \n" +
                        "                    /                     \\                     \n" +
                        "                   /                       \\                    \n" +
                        "                  /                         \\                   \n" +
                        "                 /                           \\                  \n" +
                        "                /                             \\                 \n" +
                        "               /                               \\                \n" +
                        "               +                               +                 \n" +
                        "              / \\                             / \\               \n" +
                        "             /   \\                           /   \\              \n" +
                        "            /     \\                         /     \\             \n" +
                        "           /       \\                       /       \\            \n" +
                        "          /         \\                     /         \\           \n" +
                        "         /           \\                   /           \\          \n" +
                        "        /             \\                 /             \\         \n" +
                        "       /               \\               /               \\        \n" +
                        "       +               +               +               7           \n" +
                        "      / \\             / \\             / \\                       \n" +
                        "     /   \\           /   \\           /   \\                      \n" +
                        "    /     \\         /     \\         /     \\                     \n" +
                        "   /       \\       /       \\       /       \\                    \n" +
                        "   (1)     2       (3)     (+)      5       6      .       .       \n" +
                        "                          / \\                                   \n" +
                        "                         /   \\                                  \n" +
                        " .   .   .   .   .   .   +   3   .   .   .   .   .   .   .   .   \n" +
                        "                        / \\                                     \n" +
                        "- - - - - - - - - - - - 1 9 - - - - - - - - - - - - - - - - - - "  */

        val maxTotalLookups2 = (7 -1) + 3  // The outer(only has 6 vals) and inner
        val potentialHits = 2 + 3
        val realTotalHits = potentialHits // We expect all to succeed
        val newMisses = (maxTotalLookups2 - realTotalHits) // 9 - 5 = 4

        val cacheLocalHist2 = 0 // We are not re-using the instances
        val cacheGlobalHits2 = realTotalHits - cacheLocalHist2
        val cacheMisses2 = cacheMisses1 + newMisses  // 3 + 4
        checkStats(cacheLocalHist2, cacheGlobalHits2, cacheMisses2, memoization) // 0, 5, 7


    }
}