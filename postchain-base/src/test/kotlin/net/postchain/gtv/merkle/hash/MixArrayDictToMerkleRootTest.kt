package net.postchain.gtv.merkle.hash

import net.postchain.base.merkle.TreeHelper
import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import net.postchain.gtv.merkle.MixArrayDictToGtvBinaryTreeHelper
import net.postchain.gtv.merkle.MixArrayDictToGtvBinaryTreeHelper.expecedMerkleRoot_dict1_array4
import net.postchain.gtv.merkleHashSummary
import org.junit.Test
import kotlin.test.assertEquals

class MixArrayDictToMerkleRootTest {


    @Test
    fun test_dictWithArr_root() {
        val calculator = MerkleHashCalculatorDummy()

        val orgGtvDict = MixArrayDictToGtvBinaryTreeHelper.buildGtvDictWithSubArray4()

        // [08 +  [01 + <one>]
        //      +
        //      (0802040404050204060407) <-- Stole this from ArrayToGtvBinaryTreeHelper
        //      ] ->
        // [08 +  [01 + 6F6E65]
        //      +
        //      (0802040404050204060407)
        //      ] ->
        // [08 +  02706F66 + 0802040404050204060407] ->
        //  09 +  03717067 + 0903050505060305070508 ->
        //  09037170670903050505060305070508 ->

        val root = orgGtvDict.merkleHashSummary(calculator)
        assertEquals(expecedMerkleRoot_dict1_array4, TreeHelper.convertToHex(root.merkleHash))
    }

}