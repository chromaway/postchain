package net.postchain.base.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.base.merkle.TreeHelper.stripWhite
import net.postchain.gtv.*
import net.postchain.gtv.merkle.ArrayToGtvBinaryTreeHelper
import net.postchain.gtv.merkle.ArrayToGtvBinaryTreeHelper.expected1ElementArrayMerkleRoot
import net.postchain.gtv.merkle.ArrayToGtvBinaryTreeHelper.expected4ElementArrayMerkleRoot
import net.postchain.gtv.merkle.ArrayToGtvBinaryTreeHelper.expected7ElementArrayMerkleRoot
import net.postchain.gtv.merkle.ArrayToGtvBinaryTreeHelper.expectet7and3ElementArrayMerkleRoot
import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree
import net.postchain.gtv.merkle.proof.GtvMerkleProofTreeFactory
import net.postchain.gtv.merkle.proof.merkleHash
import net.postchain.gtv.path.GtvPath
import net.postchain.gtv.path.GtvPathFactory
import net.postchain.gtv.path.GtvPathSet
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
 * In this class we test if we can generate proofs out ofGtv array structures.
 * 1. First we test to build a proof where the value-to-be-proved a primitive type value in the array.
 * 2. Then we create a double proof (more than one value-to-be-proved in the same proof tree).
 * 3. Then we test to build a proof where the value-to-be-proved is a primitive value located in a sub-array.
 * 4. Later we test to build a proof where the value-to-be-proved is a complex type (another array)
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

class ArrayToMerkleProofTreeTest {

    val proofFactory =GtvMerkleProofTreeFactory()

    // ---------------------
    // 1. First we test to build a proof where the value-to-be-proved a primitive type value in the array.
    // ---------------------
    // -------------- Size 1 ------------

    @Test
    fun test_ArrOf1_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(0)
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf1()

        val expectedTree =
                " +   \n" +
                "/ \\ \n" +
                "*1 0000000000000000000000000000000000000000000000000000000000000000"

        val merkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHash(calculator)
        assertEquals(expected1ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        //println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=103), \n" +  // 103 =  node type is array
                "  GtvInteger(integer=1), \n" +  // lenght of array
                "  GtvInteger(integer=-10),\n" + // (no path/position given)
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=101), \n" + // 101 = value to prove
                "    GtvInteger(integer=0), \n" + //path/position = 0
                "    GtvInteger(integer=1)\n" + // Actual value
                "  ]), \n" +
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=100), \n" + // 100 = hash
                "    GtvByteArray(bytearray=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0])\n" +
                "  ])\n" +
                "])\n"
        Assert.assertEquals(stripWhite(expectedSerialization), stripWhite(serialize.toString())) // Not really needed, Can be removed

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
    fun test_Arrof4_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(0)
        val gtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf4()

        // This is how the (dummy = +1) hash calculation works done for the right side of the path:
        //
        // 00 + [(01 + [03]) + (01 + [04])]
        // 00 + [(01 + 04) + (01 + 05)]
        // 00 + [0104 + 0105] <-- Now we have the hash of the leaf "3" (=0104) and leaf "4" (0105).
        // 00 + [01040105]
        // 00 + 02050206
        // 0002050206  <-- Combined hash of 3 and 4.

        val expectedTree =
                "   +       \n" +
                "  / \\   \n" +
                " /   \\  \n" +
                " +   0103050306   \n" +
                "/ \\     \n" +
                "*1 0203 - - "

        val merkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHash(calculator)
        assertEquals(expected4ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        //println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=103), \n" +// 103 = array head node type
                "  GtvInteger(integer=4), \n" + // length of array
                "  GtvInteger(integer=-10), \n" + // no path elem
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=102), \n" +
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=101), \n" +// 101 = value to prove
                "      GtvInteger(integer=0), \n" + // path elem = 0
                "      GtvInteger(integer=1)\n" +
                "    ]), \n" +
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=100), \n" +// 100 = hash
                "      GtvByteArray(bytearray=[2, 3])\n" +
                "    ])\n" +
                "  ]), \n" +
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=100), \n" +// 100 = hash
                "    GtvByteArray(bytearray=[1, 3, 5, 3, 6])\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(stripWhite(expectedSerialization), stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = proofFactory.deserialize(serialize)


        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        //println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())

    }

    // -------------- Size 7 ------------

    @Test
    fun test_ArrOf7_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(3)
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf7()

        // This is how the (dummy = +1) hash calculation works done for the right side of the path:
        //
        // 00 + [
        //        (00 + [(01 + [05]) +
        //               (01 + [06])])
        //        +
        //        (01 + [07])
        //      ]
        // 00 + [(00 + [0106 + 0107]) + 0108 ]
        // 00 + [00 + 02070208 + 0108 ]
        // 00 + [00020702080108 ]
        // 00 +  01030803090209
        val expectedTree = "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       0102040804090309       \n" +
                "  / \\           \n" +
                " /   \\          \n" +
                " 0103030304   +   .   .   \n" +
                "    / \\         \n" +
                "- - 0204 *4 - - - - "

        val merkleProofTree:GtvMerkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHash(calculator)
        assertEquals(expected7ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        //println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=103),\n" + // 103 = array head node type
                "  GtvInteger(integer=7),\n" + // length of array
                "  GtvInteger(integer=-10),\n" + // no path elem
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=102),\n" + // 102 = dummy node
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=100),\n" + // 100 = hash
                "      GtvByteArray(bytearray=[1, 3, 3, 3, 4])]),\n" +
                "      GtvArray(array=[\n" +
                "        GtvInteger(integer=102),\n" + // 102 = dummy node
                "        GtvArray(array=[\n" +
                "          GtvInteger(integer=100),\n" +  // 100 = hash
                "          GtvByteArray(bytearray=[2, 4])\n" +
                "        ]),\n" +
                "        GtvArray(array=[\n" +
                "          GtvInteger(integer=101),\n" + // 101 = value to prove
                "          GtvInteger(integer=3),\n" + // path elem = 3
                "          GtvInteger(integer=4)\n" +
                "        ])\n" +
                "      ])\n" +
                "    ]),\n" +
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=100),\n" + // 100 = hash
                "      GtvByteArray(bytearray=[1, 2, 4, 8, 4, 9, 3, 9])\n" +
                "    ])\n" +
                "  ])\n"

        Assert.assertEquals(stripWhite(expectedSerialization), stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = proofFactory.deserialize(serialize)

        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        //println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())
    }

    // ---------------------
    // 2. Then we create a double proof (more than one value-to-be-proved in the same proof tree).
    // ---------------------

    @Test
    fun test_tree_of7_with_double_proof() {
        val calculator = MerkleHashCalculatorDummy()

        val path1: Array<Any> = arrayOf(3)
        val path2: Array<Any> = arrayOf(6)
        val gtvPath1: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path1)
        val gtvPath2: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path2)
        val gtvPaths = GtvPathSet(setOf(gtvPath1, gtvPath2))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrayOf7()

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

        val merkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHash(calculator)
        assertEquals(expected7ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))
    }

    // ---------------------
    // 3. Then we test to build a proof where the value-to-be-proved is a primitive value located in a sub-array.
    // ---------------------
    // -------------- Size 7 with inner 3 ------------

    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2nine() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(3,1)
        val gtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrOf7WithInner3()


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


        val merkleProofTree:GtvMerkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHash(calculator)
        assertEquals(expectet7and3ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))
    }


    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2three() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(2)
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrOf7WithInner3()

        // How to calculate the hash of the sub tree?
        // see test_ArrayLength7_withInnerLength3Array_root
        // == 08020404040C0305

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


        val merkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHash(calculator)
        assertEquals(expectet7and3ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))
    }


    // ---------------------
    // 4. Later we test to build a proof where the value-to-be-proved is a complex type (another array)
    // ---------------------

    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2subArray() {
        val calculator = MerkleHashCalculatorDummy()

        val path: Array<Any> = arrayOf(3)
        val gtvPath: GtvPath = GtvPathFactory.buildFromArrayOfPointers(path)
        val gtvPaths = GtvPathSet(setOf(gtvPath))
        val orgGtvArr = ArrayToGtvBinaryTreeHelper.buildGtvArrOf7WithInner3()

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


        val merkleProofTree = orgGtvArr.generateProof(gtvPaths, calculator)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Make sure the merkle root stays the same as without proof
        val merkleProofRoot = merkleProofTree.merkleHash(calculator)
        assertEquals(expectet7and3ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=103), \n" + // 103 = array head node type
                "  GtvInteger(integer=7), \n" + // length of array
                "  GtvInteger(integer=-10), \n" + // no path elem
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=102), \n" + // 102 = dummy node
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=100), \n" + // 100 = hash
                "      GtvByteArray(bytearray=[1, 3, 3, 3, 4])\n" +
                "    ]),\n" +
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=102), \n" + // 102 = dummy node
                "      GtvArray(array=[\n" +
                "        GtvInteger(integer=100), \n" + // 100 = hash
                "        GtvByteArray(bytearray=[2, 4])\n" +
                "      ]), \n" +
                "      GtvArray(array=[\n" +
                "        GtvInteger(integer=101), \n" + // 101 = value to prove
                "        GtvInteger(integer=3), \n" + // path elem = 2
                "        GtvArray(array=[\n" +  // Here the value to prove is a regular GtvArray. Interesting to see that this is deserialized propely (i.e. kept)
                "          GtvInteger(integer=1), \n" +
                "          GtvInteger(integer=9), \n" +
                "          GtvInteger(integer=3)\n" +
                "        ])\n" +
                "      ])\n" +
                "    ])\n" +
                "  ]), \n" +
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=100), \n" + // 100 = hash
                "    GtvByteArray(bytearray=[1, 2, 4, 8, 4, 9, 3, 9])\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(stripWhite(expectedSerialization), stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = proofFactory.deserialize(serialize)


        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())

    }
}