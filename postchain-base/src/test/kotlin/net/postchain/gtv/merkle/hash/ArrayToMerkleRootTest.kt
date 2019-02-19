package net.postchain.gtv.merkle.hash

import net.postchain.base.merkle.TreeHelper
import net.postchain.gtv.merkle.ArrayToGtvBinaryTreeHelper
import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import net.postchain.gtv.merkleHashSummary
import org.junit.Test
import kotlin.test.assertEquals

/**
 * In this class we test if we can calculate merkle roots out of Gtv array structures.
 *
 * -----------------
 * How to read the tests
 * -----------------
 *
 * In the comments below
 *   "<...>" means "serialization" and
 *   "[ .. ]" means "hash" and
 *   "(a + b)" means append "b" after "a" into "ab"
 *
 * Since we are using the dummy hash function, all binary numbers will be +1 after hash function
 *  02 -> 03 etc.
 *
 * The dummy serializer doesn't do anything but converting an int to a byte:
 *   7 -> 07
 *   12 -> 0C
 *
 * -----------------
 *
 * Note: We are not testing the cache, so every test begins with a fresh Calculator (and therefore a fresh cache).
 */

class ArrayToMerkleRootTest {

    @Test
    fun test_ArrOf1_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf1()

        val merkleProofRoot = orgGtvArr.merkleHashSummary(calculator)
        assertEquals(ArrayToGtvBinaryTreeHelper.expected1ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))
    }

    @Test
    fun test_ArrOf4_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf4()

        // (see the test above for where we get these numbers)
        // 07 + [
        //       00 [(01 + [<1>]) + 0103] +
        //       0002050206
        //      ] ->
        // 07 + [
        //       00 [0102 + 0103] +
        //       0002050206
        //      ] ->
        // 07 + [ 0002030204 + 0002050206 ] ->
        // 07     0103040305 + 0103060307 ->
        // 0701030403050103060307

        val merkleProofRoot = orgGtvArr.merkleHashSummary(calculator)
        assertEquals(ArrayToGtvBinaryTreeHelper.expected4ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))
    }

    @Test
    fun test_ArrOf7_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf7()

        // 07 + [
        //      00 [
        //         00 [<1>  <2>]
        //       + 00 [<3>  <4>]
        //    + 00 [
        //         00 [<5>  <6>]
        //       + <7>]
        //     ]
        // 07 + [00 [00 [0102 + 0103] + 00 [0104 + 0105]]
        //     + 00 [00 [0106 + 0107] + 0108] ]
        // 07 + [00 [0002030204 + 0002050206]
        //     + 00 [0002070208 + 0108] ]
        // 07 + [00 + 0103040305 + 0103060307 + 00 + 0103080309 + 0209]
        // 07 0102040504060204070408 + 01020409040A030A
        // 07010204050406020407040801020409040A030A

        val merkleProofRoot = orgGtvArr.merkleHashSummary(calculator)
        assertEquals(ArrayToGtvBinaryTreeHelper.expected7ElementArrayMerkleRoot , TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))
    }

    @Test
    fun test_ArrayLength7_withInnerLength3Array_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrOf7WithInner3()

        val merkleProofRoot = orgGtvArr.merkleHashSummary(calculator)
        assertEquals(ArrayToGtvBinaryTreeHelper.expectet7and3ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))
    }
}