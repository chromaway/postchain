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
        assertEquals(ArrayToGtvBinaryTreeHelper.expected1ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot.merkleHash))
    }

    @Test
    fun test_ArrOf4_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf4()

        // (see the test above for where we get these numbers)
        // [07 +
        //       [00 [01 + <1>] + 0203] +
        //       0103050306
        //      ] ->
        // [07 +
        //       [00 0202 + 0203] +
        //       0103050306
        //      ] ->
        // [07 +  0103030304 + 0103050306 ] ->
        // 08     0204040405 + 0204060407 ->
        // 0802040404050204060407

        val merkleProofRoot = orgGtvArr.merkleHashSummary(calculator)
        assertEquals(ArrayToGtvBinaryTreeHelper.expected4ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot.merkleHash))
    }

    @Test
    fun test_ArrOf7_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf7()

        // [07 +
        //      [00
        //         [00 [01 <1>]  [01 <2>]]
        //       + [00 [01 <3>]  [01 <4>]]
        //    + [00
        //         [00 [01 <5>]  [01 <6>]]
        //       +               [01 <7>]  ]
        //     ]
        // [07 + [00 [00 + 0202 + 0203] + [00 + 0204 + 0205]]
        //     + [00 [00 + 0206 + 0207] +              0208 ] ]
        // [07 + [00+ 0103030304        +         0103050306]
        //     + [00+ 0103070308        +              0208 ] ]
        // [07 +  01+ 0204040405        +         0204060407
        //     +  01+ 0204080409 +                     0309   ]
        // 08     02  0305050506                  0305070508
        //     +  02  030509050A                       040A
        // 08020305050506030507050802030509050A040A

        val merkleProofRoot = orgGtvArr.merkleHashSummary(calculator)
        assertEquals(ArrayToGtvBinaryTreeHelper.expected7ElementArrayMerkleRoot , TreeHelper.convertToHex(merkleProofRoot.merkleHash))
    }

    @Test
    fun test_ArrayLength7_withInnerLength3Array_root() {

        // Inner array
        //"   / \    \n" +
        //"  /   \   \n" +
        //"  +   3   \n" +
        //" / \      \n" +
        //" 1 9        "

        // [07 +
        //       [00 [01 + <1>] + [01 + <9>]]
        //     +     [01 + <3>]
        //      ] ->
        // [07 +
        //       [00 0202 + 020A]
        //     +     0204
        //      ] ->
        // [07 + 01 0303 + 030B
        //     +     0204 ] ->
        // 08 + 02 0404 + 040C  + 0305  ->
        // 08020404040C0305

        // We have to hash this inner array 3 times
        // [[[08020404040C0305]]]
        // [[ 09030505050D0406 ]]
        // [  0A040606060E0507  ]
        //    0B050707070F0608 <- this is what we call "inner3arrayHash" in the helper


        val calculator = MerkleHashCalculatorDummy()

        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrOf7WithInner3()

        val merkleProofRoot = orgGtvArr.merkleHashSummary(calculator)
        assertEquals(ArrayToGtvBinaryTreeHelper.expectet7and3ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot.merkleHash))
    }
}