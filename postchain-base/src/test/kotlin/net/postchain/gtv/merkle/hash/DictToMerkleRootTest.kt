package net.postchain.gtv.merkle.hash

import net.postchain.base.merkle.TreeHelper
import net.postchain.gtv.merkle.DictToGtvBinaryTreeHelper
import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import net.postchain.gtv.merkleHashSummary
import org.junit.Test
import kotlin.test.assertEquals


/**
 * In this class we test if we can calculate merkle roots out of Gtv dictionary structures.
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
class DictToMerkleRootTest {

    @Test
    fun test_dict1_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1()
        // 08 + [01706F66 + (01 + [01])
        // 08 + [01706F66 + 0102]
        // 08 + 02717067 + 0203

        val merkleProofRoot = orgGtvDict.merkleHashSummary(calculator)
        assertEquals(DictToGtvBinaryTreeHelper.expectedMerkleRoot1, TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))
    }

    @Test
    fun test_dict4_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf4()

        // 08 + [
        //        (00 + [
        //                (00 + [0167707673 + 0105]
        //                +
        //                00027170670203
        //              ])
        //        +
        //        000103776B75686803060103777A720305
        //       ] ->
        // 08 + [
        //        (00 + [
        //                (00 + 02687177740206)
        //                +
        //                00027170670203
        //              ])
        //        +
        //        000103776B75686803060103777A720305
        //       ] ->
        // 08 + [
        //        (00 + [ 000268717774020600027170670203 ])
        //        +
        //        000103776B75686803060103777A720305
        //       ] ->
        // 08 + [
        //        (00 + 010369727875030701037271680304)
        //        +
        //        000103776B75686803060103777A720305
        //       ] ->
        //  080102046A737976040802047372690405010204786C76696904070204787B730406

        val merkleProofRoot = orgGtvDict.merkleHashSummary(calculator)
        assertEquals(DictToGtvBinaryTreeHelper.expectedMerkleRoot4, TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))
    }

    @Test
    fun test_dictOfDict_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1WithSubDictOf2()

        // 08 + [ (01 + [<one>])
        //      +
        //      (08 + [
        //            (00 + [
        //                 (01 + [<eight>]) +  <--- "e" (Eight) is before "s" (Seven)
        //                 (01 + [<8>])
        //                  ])
        //             +
        //            (00 + [
        //                 (01 + [<seven>]) +
        //                 (01 + [<7>])
        //                  ])
        //            ])
        //      ] ->
        // <eight> = <6569676874> = 666A686975
        // <seven> = <736576656E> = 746677666F
        // 08 + [ 01 + [6F6E65]
        //      +
        //      (08 + [
        //            (00 + [01666A686975 + 0109])
        //             +
        //            (00 + [01746677666F + 0108])
        //            ])
        //      ] ->
        //
        // 08 + [ 01 + 706F66
        //        +
        //       (08 + [ 0002676B696A76020A + 000275677867700209])
        //      ] ->
        //
        // 08 + [ 01706F66
        //        +
        //       (08 +0103686C6A6B77030B + 01037668796871030A)
        //      ] ->
        //
        // 08 + [ 01706F66+ 08 + 0103686C6A6B77030B + 01037668796871030A] ->
        //
        // 08 02717067 09 0204696D6B6C78040C 020477697A6972040B

        val merkleProofRoot = orgGtvDict.merkleHashSummary(calculator)
        assertEquals(DictToGtvBinaryTreeHelper.expectedMerkleRootDictInDict, TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))
    }


}