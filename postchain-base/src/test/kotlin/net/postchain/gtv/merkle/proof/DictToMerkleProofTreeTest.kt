package net.postchain.base.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.gtv.*
import net.postchain.gtv.merkle.DictToGtvBinaryTreeHelper
import net.postchain.gtv.merkle.DictToGtvBinaryTreeHelper.expectedMerkleRoot1
import net.postchain.gtv.merkle.DictToGtvBinaryTreeHelper.expectedMerkleRoot4
import net.postchain.gtv.merkle.DictToGtvBinaryTreeHelper.expectedMerkleRootDictInDict
import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import net.postchain.gtv.merkle.proof.GtvMerkleProofTreeFactory
import net.postchain.gtv.merkle.proof.merkleHashSummary
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
 * In this class we test if we can generate proofs out ofGtv dictionary structures.
 * 1. First we test to build a proof where the value-to-be-proved a primitive type value in the dict.
 * 2. Then we test to build a proof where the value-to-be-proved is a primitive value located in a sub-dict.
 * 3. Later we test to build a proof where the value-to-be-proved is a complex type (another dict)
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
 * Note1: The testing of the exact serialization format is not very important and can be removed. The only important
 * thing is that we keep testing after deserialization.
 *
 * Note2: We are not testing the cache, so every test begins with a fresh Calculator (and therefore a fresh cache).
 */

class DictToMerkleProofTreeTest {

    val proofFactory =GtvMerkleProofTreeFactory()


    // ---------------------
    // 1. First we test to build a proof where the value-to-be-proved a primitive type value in the dict.
    // ---------------------

    // -------------- Size 1 ------------

    @Test
    fun test_dict1_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf("one")
        val gtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1()

        // How to convert one to hash?:
        // "one" ->(serialization) 6F6E65
        // 01 + [6F6E65] ->
        // 01706F66 ->
        val expectedTree =" +   \n" +
                "/ \\ \n" +
                "01706F66 *1 "

        val merkleProofTree = orgGtvDict.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHashSummary(calculator)
        assertEquals(expectedMerkleRoot1, TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        //println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=104),\n" + // 104 = dict head node type
                "  GtvInteger(integer=1),\n" + // length of dict
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=100),\n" + // 100 = hash
                "    GtvByteArray(bytearray=[1, 112, 111, 102])\n" +
                "  ]),\n" +
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=101),  \n" + // 101 = value to prove
                "    GtvInteger(integer=1)\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(TreeHelper.stripWhite(expectedSerialization), TreeHelper.stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = proofFactory.deserialize(serialize)

        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        //println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())
    }


    // -------------- Size 4 ------------

    @Test
    fun test_dict4_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf("four")
        val gtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvPath = DictToGtvBinaryTreeHelper.buildGtvDictOf4()

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
        //            (01 + [<three>]) +   <-- "th" is before "tw"
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
        val expectedTree =
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

        val merkleProofTree = orgGtvPath.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHashSummary(calculator)
        assertEquals(expectedMerkleRoot4, TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        //println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=104), \n" + // 104 = dict head node type
                "  GtvInteger(integer=4), \n" + // length of the dict
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=102), \n" + // 102 = dummy node
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=102), \n" + // 102 = dummy node
                "      GtvArray(array=[\n" +
                "        GtvInteger(integer=100), \n" + // 100 = hash
                "        GtvByteArray(bytearray=[1, 103, 112, 118, 115])\n" +
                "      ]),\n" +
                "      GtvArray(array=[\n" +
                "        GtvInteger(integer=101),   \n" + // 101 = value to prove
                "        GtvInteger(integer=4)\n" +
                "      ])\n" +
                "    ]),  \n" +
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=100),   \n" + // 100 = hash
                "      GtvByteArray(bytearray=[0, 2, 113, 112, 103, 2, 3])\n" +
                "    ])\n" +
                "  ]),   \n" +
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=100),   \n" + // 100 = hash
                "    GtvByteArray(bytearray=[0, 1, 3, 119, 107, 117, 104, 104, 3, 6, 1, 3, 119, 122, 114, 3, 5])\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(TreeHelper.stripWhite(expectedSerialization), TreeHelper.stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = proofFactory.deserialize(serialize)

        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        //println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())

    }


    // ---------------------
    // 2. Then we test to build a proof where the value-to-be-proved is a primitive value located in a sub-dict.
    // ---------------------

    @Test
    fun test_dictOfDict_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf("one", "seven")
        val gtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1WithSubDictOf2()

        val expectedTree = "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   01706F66       +       \n" +
                "          / \\   \n" +
                "         /   \\  \n" +
                " .   .   0002676B696A76020A   +   \n" +
                "            / \\ \n" +
                "- - - - - - 01746677666F *7 "

        val merkleProofTree = orgGtvDict.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHashSummary(calculator)
        assertEquals(expectedMerkleRootDictInDict, TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        println("Serilalized: $serialize")

        val expectedSerialization =  "GtvArray(array=[\n" +
                "  GtvInteger(integer=104), \n" + // 104 = dict head node type
                "  GtvInteger(integer=1), \n" + // length of the dict
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=100),\n" +
                "    GtvByteArray(bytearray=[1, 112, 111, 102])\n" +
                "  ]),\n" +
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=104), \n" + // 104 = dict head node type
                "    GtvInteger(integer=2), \n" + // length of the dict
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=100), \n" + // 100 = hash
                "      GtvByteArray(bytearray=[0, 2, 103, 107, 105, 106, 118, 2, 10])\n" +
                "    ]), \n" +
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=102), \n" + // 102 = dummy node
                "      GtvArray(array=[\n" +
                "        GtvInteger(integer=100), \n" + // 100 = hash
                "        GtvByteArray(bytearray=[1, 116, 102, 119, 102, 111])\n" +
                "      ]), \n" +
                "      GtvArray(array=[\n" +
                "        GtvInteger(integer=101), \n" + // 101 = value to prove
                "        GtvInteger(integer=7)\n" +
                "      ])\n" +
                "    ])\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(TreeHelper.stripWhite(expectedSerialization), TreeHelper.stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = proofFactory.deserialize(serialize)

        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())

    }

    // ---------------------
    // 3. Later we test to build a proof where the value-to-be-proved is a complex type (another dict)
    // ---------------------

    /**
     * This test will create a proof of a sub-dictionary inside the main dictionary.
     *
     * Note: This test depend on the auto-generated output of toString() of the "data class" of theGtv Dict.
     */
    @Test
    fun test_dictOfDict_proof_where_path_is_to_sub_dict() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf("one")
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvDict = DictToGtvBinaryTreeHelper.buildGtvDictOf1WithSubDictOf2()

        val expectedTree = " +   \n" +
                "/ \\ \n" +
                "01706F66 *GtvDictionary(dict={seven=GtvInteger(integer=7), eight=GtvInteger(integer=8)}) "


        val merkleProofTree = orgGtvDict.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHashSummary(calculator)
        assertEquals(expectedMerkleRootDictInDict, TreeHelper.convertToHex(merkleProofRoot.getHashWithPrefix()))

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        //println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=104),\n" +  // 104 = dict head node type
                "  GtvInteger(integer=1),\n" + // lenght of the dict
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=100),\n" + // 100 = Hash
                "    GtvByteArray(bytearray=[1, 112, 111, 102])\n" +
                "  ]),\n" +
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=101), \n" + // 101 = value to be proved (in this case an entire dict)
                "    GtvDictionary(dict={\n" +  // The value is a GtvDictionary, in it's raw form
                "      seven=GtvInteger(integer=7), \n" +
                "      eight=GtvInteger(integer=8)\n" +
                "    })\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(TreeHelper.stripWhite(expectedSerialization), TreeHelper.stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = proofFactory.deserialize(serialize)

        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        //println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())


    }




}