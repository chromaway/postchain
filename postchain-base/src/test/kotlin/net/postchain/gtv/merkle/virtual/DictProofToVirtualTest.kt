package net.postchain.gtv.merkle.virtual

import net.postchain.base.merkle.TreeHelper
import net.postchain.gtv.generateProof
import net.postchain.gtv.merkle.DictToGtvBinaryTreeHelper
import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import net.postchain.gtv.merkle.proof.toGtvVirtual
import net.postchain.gtv.merkleHash
import net.postchain.gtv.path.GtvPath
import net.postchain.gtv.path.GtvPathFactory
import net.postchain.gtv.path.GtvPathSet
import org.junit.Test
import kotlin.test.assertEquals


class DictProofToVirtualTest {



    // -------------- Size 1 ------------

    @Test
    fun test_dict1_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf("one")
        val gtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1()

        val merkleProofTree = orgGtvDict.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()
        val merkleRoot = virtualGtv.merkleHash(calculator)
        assertEquals(DictToGtvBinaryTreeHelper.expectedMerkleRoot1, TreeHelper.convertToHex(merkleRoot))

        val orgGtv = orgGtvDict["one"]!!
        val gtvFromVirt =virtualGtv["one"]!!
        assertEquals(orgGtv, gtvFromVirt)
    }


    // -------------- Size 4 ------------

    @Test
    fun test_dict4_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf("four")
        val gtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf4()

        /*
        val expectedTree =
                "       +               \n" +
                        "      / \\       \n" +
                        "     /   \\      \n" +
                        "    /     \\     \n" +
                        "   /       \\    \n" +
                        "   +       010204776B75686804060204777A720405       \n" +
                        "  / \\           \n" +
                        " /   \\          \n" +
                        " +   01037170670303   .   .   \n" +
                        "/ \\             \n" +
                        "0267707673 *4 - - - - - - "
                        */

        val merkleProofTree = orgGtvDict.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()
        val merkleRoot = virtualGtv.merkleHash(calculator)
        assertEquals(DictToGtvBinaryTreeHelper.expectedMerkleRoot4, TreeHelper.convertToHex(merkleRoot))

        val orgGtv = orgGtvDict["four"]!!
        val gtvFromVirt =virtualGtv["four"]!!
        assertEquals(orgGtv, gtvFromVirt)

    }


    @Test
    fun test_dictOfDict_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf("one", "seven")
        val gtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1WithSubDictOf2()

        /*
        val expectedTree = "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   02706F66       *       \n" +
                "          / \\   \n" +
                "         /   \\  \n" +
                " .   .   0103676B696A76030A   +   \n" +
                "            / \\ \n" +
                "- - - - - - 02746677666F *7 "
                */

        val merkleProofTree = orgGtvDict.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()
        val merkleRoot = virtualGtv.merkleHash(calculator)
        assertEquals(DictToGtvBinaryTreeHelper.expectedMerkleRootDictInDict, TreeHelper.convertToHex(merkleRoot))

        val orgGtv = orgGtvDict["one"]!!["seven"]!!
        val gtvFromVirt =virtualGtv["one"]!!["seven"]!!
        assertEquals(orgGtv, gtvFromVirt)
    }

    /**
     * This test will create a proof of a sub-dictionary inside the main dictionary.
     */
    @Test
    fun test_dictOfDict_proof_where_path_is_to_sub_dict() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf("one")
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1WithSubDictOf2()

        /*
        val expectedTree = " +   \n" +
                "/ \\ \n" +
                "02706F66 *GtvDictionary(dict={seven=GtvInteger(integer=7), eight=GtvInteger(integer=8)}) "
                */


        val merkleProofTree = orgGtvDict.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()
        val merkleRoot = virtualGtv.merkleHash(calculator)
        assertEquals(DictToGtvBinaryTreeHelper.expectedMerkleRootDictInDict, TreeHelper.convertToHex(merkleRoot))

        val orgGtv = orgGtvDict["one"]!!
        val gtvFromVirt =virtualGtv["one"]!!
        assertEquals(orgGtv, gtvFromVirt)
    }




}

