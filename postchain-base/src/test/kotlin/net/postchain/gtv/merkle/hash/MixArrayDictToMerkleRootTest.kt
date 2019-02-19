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

        // 08 + [ (01 + [<one>])
        //      +
        //      (0701030403050103060307) <-- Stole this from test "test_tree_of4_merkle_root()"
        //      ] ->
        // 08 + [ 01 + [6F6E65]
        //      +
        //      (0701030403050103060307)
        //      ] ->
        // 08 + [ 01 + 706F66 + 0701030403050103060307] ->
        // 08 + 02 + 717067 + 0802040504060204070408 ->
        // 08027170670802040504060204070408

        val root = orgGtvDict.merkleHashSummary(calculator)
        assertEquals(expecedMerkleRoot_dict1_array4, TreeHelper.convertToHex(root.getHashWithPrefix()))
    }

}