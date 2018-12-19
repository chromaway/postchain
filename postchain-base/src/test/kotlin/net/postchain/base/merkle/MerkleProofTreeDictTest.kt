package net.postchain.base.merkle

import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXPathFactory
import net.postchain.gtx.GTXValue
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
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
 */

class MerkleProofTreeDictTest {

    val expectedMerkleRoot = "080102046A737976040802047372690405010204786C76696904070204787B730406"

    @Test
    fun test_tree_from_4dict() {
        val path: Array<Any> = arrayOf("four")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = GtxTreeDictHelper.buildThreeOf4_fromDict(gtxPath)

        // This is how the (dummy = +1) hash calculation works done for the right side of the path:
        //
        //
        // "four" serialized becomes bytes: 666F7572
        // "one" serialized becomes bytes: 6F6E65
        // "three" serialized becomes bytes: 7468726565
        // "two" serialized becomes bytes: 74776F
        //
        // 00 + [(00 +
        //         [
        //            (01 + [<three>]) +
        //            (01 + [<3>])
        //         ])
        //       (00 +
        //         [
        //            (01 + [<two>]) +
        //            (01 + [<2>])
        // 00 + [(00 +
        //         [
        //            (01 + [7468726565]) +
        //            (01 + [03])
        //         ])
        //       (00 +
        //         [
        //            (01 + [74776F]) +
        //            (01 + [02])
        //         ])
        //         ])
        // 00 + [(00 + [017569736666 + 0104)] +
        //      [(00 + [01757870 + 0103)])
        // 00 + [(00 + 02766A746767 0205)] +
        //      [(00 + 02767971 0204])
        // 00 + 01 03776B756868 0306 +
        //      01 03777A72 0305
        // 000103776B75686803060103777A720305
        //
        val expectedPath =
                "       +               \n" +
                        "      / \\       \n" +
                        "     /   \\      \n" +
                        "    /     \\     \n" +
                        "   /       \\    \n" +
                        "   +       000103776B75686803060103777A720305       \n" +
                        "  / \\           \n" +
                        " /   \\          \n" +
                        " +   00027170670203   .   .   \n" +
                        "/ \\             \n" +
                        "0167707673 *4 - - - - - - "

        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(treeHolder.clfbTree, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedPath.trim(), resultPrintout.trim())

    }

    @Test
    fun test_tree_from_4dict_proof() {
        val path: Array<Any> = arrayOf("four")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = GtxTreeDictHelper.buildThreeOf4_fromDict(gtxPath)

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

        val calculator = MerkleHashCalculatorDummy()
        val merkleProofTree: MerkleProofTree = MerkleProofTreeFactory.buildMerkleProofTree(treeHolder.clfbTree, calculator)


        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expectedMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))

    }



}