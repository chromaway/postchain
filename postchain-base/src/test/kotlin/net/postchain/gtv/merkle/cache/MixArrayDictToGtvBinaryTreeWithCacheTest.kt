package net.postchain.gtv.merkle.cache

import net.postchain.base.merkle.PrintableTreeFactory
import net.postchain.base.merkle.TreePrinter
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

    private fun buildTreeOfDict1WithSubArray4(memoization: GtvMerkleHashMemoization): String {
        val gtvDict = MixArrayDictToGtvBinaryTreeHelper.buildGtvDictWithSubArray4()

        val fullBinaryTree: GtvBinaryTree = factory.buildFromGtv(gtvDict, memoization)

        val printer = TreePrinter()
        val printableBinaryTree = PrintableTreeFactory.buildPrintableTreeFromClfbTree(fullBinaryTree)
        val treePrintout = printer.printNode(printableBinaryTree)
        //println(treePrintout)
        return treePrintout
    }

    @Test
    fun calculate_inner_array_and_then_dictWithArray() {
        val calculator = MerkleHashCalculatorDummy()
        val memoization = calculator.memoization as GtvMerkleHashMemoization

        // -------------------  Calculate inner array of 4 (just to get it into cache) ---------------------
        val gtvInnerArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf4()
        val root1 = gtvInnerArr.merkleHash(calculator)

        val maxTotalLookups = 4  // The outer GtvArray is not counted
        val cacheMisses1 = maxTotalLookups
        checkStats(0, 0, cacheMisses1, memoization)

        // ------------------- Do dict with array ---------------------
        val expectedTree =  "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   one       +       \n" +
                "          / \\   \n" +
                "         /   \\  \n" +
                " .   .   +   +   \n" +
                "        / \\ / \\ \n" +
                "- - - - (0202) (0203) (0204) (0205) "

        val treePrintout = buildTreeOfDict1WithSubArray4(calculator.memoization)
        //println(treePrintout)
        Assert.assertEquals(expectedTree.trim(), treePrintout.trim())

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