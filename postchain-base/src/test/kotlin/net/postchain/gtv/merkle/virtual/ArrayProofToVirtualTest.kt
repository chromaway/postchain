package net.postchain.gtv.merkle.virtual

import net.postchain.gtv.generateProof
import net.postchain.gtv.merkle.ArrayToGtvBinaryTreeHelper
import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree
import net.postchain.gtv.merkle.proof.toGtvVirtual
import net.postchain.gtv.path.GtvPath
import net.postchain.gtv.path.GtvPathFactory
import net.postchain.gtv.path.GtvPathSet
import org.junit.Test
import kotlin.test.assertEquals

class ArrayProofToVirtualTest {

    // -------------- Size 1 ------------

    @Test
    fun test_ArrOf1_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(0)
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf1()

        val merkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()

        val orgGtv = orgGtvArr[0]
        val gtvFromVirt =virtualGtv[0]
        assertEquals(orgGtv, gtvFromVirt)
    }

    // -------------- Size 4 ------------

    @Test
    fun test_Arrof4_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(0)
        val gtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf4()

        val merkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()

        val orgGtv = orgGtvArr[0]
        val gtvFromVirt =virtualGtv[0]
        assertEquals(orgGtv, gtvFromVirt)

    }

    // -------------- Size 7 ------------

    @Test
    fun test_ArrOf7_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(3)
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf7()


        val merkleProofTree: GtvMerkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()

        val orgGtv = orgGtvArr[3]
        val gtvFromVirt =virtualGtv[3]
        assertEquals(orgGtv, gtvFromVirt)
    }

    @Test
    fun test_tree_of7_with_double_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path1: Array<Any> = arrayOf(3)
        val path2: Array<Any> = arrayOf(6)
        val gtvPath1: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path1)
        val gtvPath2: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path2)
        val gtvPaths = GtvPathSet(setOf(gtvPath1, gtvPath2))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf7()

        /*
        val expectedTree =
                "       +               \n" +
                        "      / \\       \n" +
                        "     /   \\      \n" +
                        "    /     \\     \n" +
                        "   /       \\    \n" +
                        "   +       +       \n" +
                        "  / \\     / \\   \n" +
                        " /   \\   /   \\  \n" +
                        " 0103030304   +   0103070308   *7   \n" +
                        "    / \\         \n" +
                        "- - 0204 *4 - - - - "
                        */

        val merkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()

        val orgGtv = orgGtvArr[3]
        val gtvFromVirt =virtualGtv[3]
        assertEquals(orgGtv, gtvFromVirt)

        val orgGtv1 = orgGtvArr[6]
        val gtvFromVirt1 =virtualGtv[6]
        assertEquals(orgGtv1, gtvFromVirt1)

    }

    // -------------- Size 7 with inner 3 ------------

    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2nine() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(3,1)
        val gtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrOf7WithInner3()


        /*
        val expectedTree =
                "                               +                                                               \n" +
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
                        "               +                               0102040804090309                               \n" +
                        "              / \\                                               \n" +
                        "             /   \\                                              \n" +
                        "            /     \\                                             \n" +
                        "           /       \\                                            \n" +
                        "          /         \\                                           \n" +
                        "         /           \\                                          \n" +
                        "        /             \\                                         \n" +
                        "       /               \\                                        \n" +
                        "       0103030304               +               .               .               \n" +
                        "                      / \\                                       \n" +
                        "                     /   \\                                      \n" +
                        "                    /     \\                                     \n" +
                        "                   /       \\                                    \n" +
                        "   .       .       0204       *       .       .       .       .       \n" +
                        "                          / \\                                   \n" +
                        "                         /   \\                                  \n" +
                        " .   .   .   .   .   .   +   0204   .   .   .   .   .   .   .   .   \n" +
                        "                        / \\                                     \n" +
                        "- - - - - - - - - - - - 0202 *9 - - - - - - - - - - - - - - - - - - "

*/

        val merkleProofTree: GtvMerkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()

        val orgGtv = orgGtvArr[3][1]
        val gtvFromVirt =virtualGtv[3][1]
        assertEquals(orgGtv, gtvFromVirt)

   }


    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2three() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(2)
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrOf7WithInner3()

        /*
        val expectedTree =
                "       +               \n" +
                        "      / \\       \n" +
                        "     /   \\      \n" +
                        "    /     \\     \n" +
                        "   /       \\    \n" +
                        "   +       0102040804090309       \n" +
                        "  / \\           \n" +
                        " /   \\          \n" +
                        " 0103030304   +   .   .   \n" +
                        "    / \\         \n" +
                        "- - *3 08020404040C0305 - - - - "
*/

        val merkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()

        val orgGtv = orgGtvArr[2]
        val gtvFromVirt =virtualGtv[2]
        assertEquals(orgGtv, gtvFromVirt)
    }


    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2subArray() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(3)
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrOf7WithInner3()

        /*
        val expectedTree ="       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       0102040804090309       \n" +
                "  / \\           \n" +
                " /   \\          \n" +
                " 0103030304   +   .   .   \n" +
                "    / \\         \n" +
                "- - 0204 *GtvArray(array=[GtvInteger(integer=1), GtvInteger(integer=9), GtvInteger(integer=3)]) - - - - "
                */


        val merkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        val virtualGtv = merkleProofTree.toGtvVirtual()

        val orgGtv = orgGtvArr[3]
        val gtvFromVirt =virtualGtv[3]
        assertEquals(orgGtv, gtvFromVirt)

    }
}