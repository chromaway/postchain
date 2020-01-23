// Copyright (c) 2020 ChromaWay AB. See README for license information.

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
        // one =   6F6E65
        // ---------------
        // [08 + [01 <one>] + [01 + <1>]
        // [08 + 02706F66 +  0202]
        //  09 + 03717067 +  0303

        val merkleProofRoot = orgGtvDict.merkleHashSummary(calculator)
        assertEquals(DictToGtvBinaryTreeHelper.expectedMerkleRoot1, TreeHelper.convertToHex(merkleProofRoot.merkleHash))
    }

    @Test
    fun test_dict4_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf4()

        // four =  666F7572
        // one =   6F6E65
        // three = 7468726565
        // two =   74776F
        // ---------------
        // [08 +
        //        [00 +
        //                [00 + [01 +<four>] + [01 +<4>]]
        //                +
        //                [00 + [01 +<one>]  + [01 +<1>]]
        //        ]
        //        +
        //        [00 +
        //                [00 + [01 +<three>] + [01 +<3>]]
        //                +
        //                [00 + [01 +<two>]   + [01 +<2>]]
        //        ]
        //    ] ->
        // [08 +
        //        [00 +
        //                [00 + 02 67707673 + 0205]
        //                +
        //                [00 + 02 706F66   + 0202]
        //              ]
        //        +
        //        [00 +
        //                [00 + 02 7569736666 + 0204]
        //                +
        //                [00 + 02 757870     + 0203]
        //       ] ->
        // [08 +
        //        [00 +    01 + 0368717774 + 0306   + 01 + 03717067 + 0303 ]
        //        +
        //        [00 +    01 + 03766A746767 + 0305 + 01 + 03767971 + 0304 ]
        //       ] ->
        // [08 +
        //        01 +    02 + 0469727875 + 0407   + 02 + 04727168 + 0404
        //        +
        //        01 +    02 + 04776B756868 + 0406 + 02 + 04777A72 + 0405
        //       ] ->
        //  09 +
        //        02 +    03 + 056A737976 + 0508   + 03 + 05737269 + 0505
        //        +
        //        02 +    03 + 05786C766969 + 0507 + 03 + 05787B73 + 0506
        //        ->
        //  090203056A737976050803057372690505020305786C76696905070305787B730506
        //        ->

        val merkleProofRoot = orgGtvDict.merkleHashSummary(calculator)
        assertEquals(DictToGtvBinaryTreeHelper.expectedMerkleRoot4, TreeHelper.convertToHex(merkleProofRoot.merkleHash))
    }

    @Test
    fun test_dictOfDict_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1WithSubDictOf2()

        // [08 +  [01 + <one>]
        //      +
        //      [08 +
        //            [00 +
        //                 [01 + <eight>] +  <--- "e" (Eight) is before "s" (Seven)
        //                 [01 + <8>]
        //                  ]
        //             +
        //            [00 +
        //                 [01 + <seven>] +
        //                 [01 + <7>]
        //                  ]
        //      ]
        // ] ->
        // -----------------
        // one     = 6F6E65
        // <eight> =
        //           6569676874
        // <seven> =
        //           736576656E
        // -----------------
        // [08 +  [01 + 6F6E65]
        //      +
        //      [08 +
        //            [00 +
        //                 [01 + 6569676874] +
        //                 [01 + 08]
        //                  ]
        //             +
        //            [00 +
        //                 [01 + 736576656E] +
        //                 [01 + 07]
        //                  ]
        //      ]
        // ] ->
        //
        // [08 + 02706F66
        //      +
        //      [08 +
        //            [00 + 02 + 666A686975  + 0209]
        //             +
        //            [00 + 02 + 746677666F  + 0208]
        //      ]
        // ] ->
        //
        // [08 + 02706F66
        //      +
        //      [08 +
        //            01 + 03 + 676B696A76  + 030A
        //             +
        //            01 + 03 + 7567786770  + 0309
        //            ]
        // ] ->
        //
        // [08 + 02706F66
        //      +
        //      09 +  02 + 04 + 686C6A6B77  + 040B +
        //            02 + 04 + 7668796871  + 040A
        // ] ->
        //
        //  09 + 03717067
        //      +
        //      0A +  03 + 05 + 696D6B6C78 + 050C +
        //            03 + 05 + 77697A6972 + 050B
        //  ->
        //
        //  09037170670A0305696D6B6C78050C030577697A6972050B
        //  ->
        //
        //

        val merkleProofRoot = orgGtvDict.merkleHashSummary(calculator)
        assertEquals(DictToGtvBinaryTreeHelper.expectedMerkleRootDictInDict, TreeHelper.convertToHex(merkleProofRoot.merkleHash))
    }


}