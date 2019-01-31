package net.postchain.base.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.gtv.merkle.*
import net.postchain.base.merkle.TreeHelper.stripWhite
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvPath
import net.postchain.gtv.GtvPathFactory
import net.postchain.gtv.GtvPathSet
import net.postchain.gtv.merkle.ArrayToGtvBinaryTreeHelper
import net.postchain.gtv.merkle.MerkleHashCalculatorDummy
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree
import net.postchain.gtv.merkle.proof.GtvMerkleProofTreeFactory
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
 * Note: The testing of the exact serialization format is not very important and can be removed. The only important
 * thing is that we keep testing after deserialization.
 */
class ArrayToMerkleProofTreeTest {

    val calculator = MerkleHashCalculatorDummy()
    val factory =GtvMerkleProofTreeFactory(calculator)

    val expected1ElementArrayMerkleRoot = "0702030101010101010101010101010101010101010101010101010101010101010101"
    val expected4ElementArrayMerkleRoot = "0701030403050103060307"
    val expectet7and3ElementArrayMerkleRoot = "070102040504060204070A040607060F050801020409040A030A"


    // ---------------------
    // 1. First we test to build a proof where the value-to-be-proved a primitive type value in the array.
    // ---------------------
    @Test
    fun test_tree_of1() {
        val path: Array<Any> = arrayOf(0)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf1(gtvPath)

        val expectedTree =
                " +   \n" +
                "/ \\ \n" +
                "*1 0000000000000000000000000000000000000000000000000000000000000000 "

        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        //println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=103), \n" +  // 103 =  node type is array
                "  GtvInteger(integer=1), \n" +  // lenght of array
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=101), \n" + // 101 = value to prove
                "    GtvInteger(integer=1)\n" +
                "  ]), \n" +
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=100), \n" + // 100 = hash
                "    GtvByteArray(bytearray=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0])\n" +
                "  ])\n" +
                "])\n"
        Assert.assertEquals(stripWhite(expectedSerialization), stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = factory.deserialize(serialize)


        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        //println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())


    }

    @Test
    fun test_tree_of1_merkle_root() {
        val path: Array<Any> = arrayOf(0)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf1(gtvPath)

        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expected1ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))
    }

    @Test
    fun test_tree_of4() {
        val path: Array<Any> = arrayOf(0)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf4(gtvPath)

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
                " +   0002050206   \n" +
                "/ \\     \n" +
                "*1 0103 - - "


        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        //println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=103), \n" +// 103 = array head node type
                "  GtvInteger(integer=4), \n" + // length of array
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=102), \n" +
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=101), \n" +// 101 = value to prove
                "      GtvInteger(integer=1)\n" +
                "    ]), \n" +
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=100), \n" +// 100 = hash
                "      GtvByteArray(bytearray=[1, 3])\n" +
                "    ])\n" +
                "  ]), \n" +
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=100), \n" +// 100 = hash
                "    GtvByteArray(bytearray=[0, 2, 5, 2, 6])\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(stripWhite(expectedSerialization), stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = factory.deserialize(serialize)


        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        //println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())

    }

    @Test
    fun test_tree_of4_merkle_root() {
        val path: Array<Any> = arrayOf(0)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf4(gtvPath)

        // How to calculate the root of the proof above:
        // (see the test above for where we get these numbers)
        // 07 + [
        //       00 [(01 + [<1>]) + 0103] +
        //       0002050206
        //      ] ->
        // 07 + [
        //       00 [0102 + 0103] +
        //       0002050206
        //      ] ->
        // 07 + [ 0002030204 + 0002050206 ] ->
        // 07     0103040305 + 0103060307 ->
        // 0701030403050103060307

        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expected4ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))
    }



    @Test
    fun test_tree_of7() {
        val path: Array<Any> = arrayOf(3)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder: TreeHolderFromArray = ArrayToGtvBinaryTreeHelper.buildTreeOf7(gtvPath)

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
                "   +       0001030803090209       \n" +
                "  / \\           \n" +
                " /   \\          \n" +
                " 0002030204   +   .   .   \n" +
                "    / \\         \n" +
                "- - 0104 *4 - - - - "

        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        //println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=103),\n" + // 103 = array head node type
                "  GtvInteger(integer=7),\n" + // length of array
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=102),\n" + // 102 = dummy node
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=100),\n" + // 100 = hash
                "      GtvByteArray(bytearray=[0, 2, 3, 2, 4])]),\n" +
                "      GtvArray(array=[\n" +
                "        GtvInteger(integer=102),\n" + // 102 = dummy node
                "        GtvArray(array=[\n" +
                "          GtvInteger(integer=100),\n" +  // 100 = hash
                "          GtvByteArray(bytearray=[1, 4])\n" +
                "        ]),\n" +
                "        GtvArray(array=[\n" +
                "          GtvInteger(integer=101),\n" + // 101 = value to prove
                "          GtvInteger(integer=4)\n" +
                "        ])\n" +
                "      ])\n" +
                "    ]),\n" +
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=100),\n" + // 100 = hash
                "      GtvByteArray(bytearray=[0, 1, 3, 8, 3, 9, 2, 9])\n" +
                "    ])\n" +
                "  ])\n"

        Assert.assertEquals(stripWhite(expectedSerialization), stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = factory.deserialize(serialize)

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
        val path1: Array<Any> = arrayOf(3)
        val path2: Array<Any> = arrayOf(6)
        val gtvPath1:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path1)
        val gtvPath2:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path2)
        val treeHolder: TreeHolderFromArray = ArrayToGtvBinaryTreeHelper.buildTreeOf7(GtvPathSet(setOf(gtvPath1,gtvPath2)))

        //Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())

        val expectedTree =
                "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       +       \n" +
                "  / \\     / \\   \n" +
                " /   \\   /   \\  \n" +
                " 0002030204   +   0002070208   *7   \n" +
                "    / \\         \n" +
                "- - 0104 *4 - - - - "

        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

    }

    // ---------------------
    // 3. Then we test to build a proof where the value-to-be-proved is a primitive value located in a sub-array.
    // ---------------------
    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2nine() {
        val path: Array<Any> = arrayOf(3,1)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf7WithSubTree(gtvPath)


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
                "               +                               0001030803090209                               \n" +
                "              / \\                                               \n" +
                "             /   \\                                              \n" +
                "            /     \\                                             \n" +
                "           /       \\                                            \n" +
                "          /         \\                                           \n" +
                "         /           \\                                          \n" +
                "        /             \\                                         \n" +
                "       /               \\                                        \n" +
                "       0002030204               +               .               .               \n" +
                "                      / \\                                       \n" +
                "                     /   \\                                      \n" +
                "                    /     \\                                     \n" +
                "                   /       \\                                    \n" +
                "   .       .       0104       +       .       .       .       .       \n" +
                "                          / \\                                   \n" +
                "                         /   \\                                  \n" +
                " .   .   .   .   .   .   +   0104   .   .   .   .   .   .   .   .   \n" +
                "                        / \\                                     \n" +
                "- - - - - - - - - - - - 0102 *9 - - - - - - - - - - - - - - - - - - "


        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())
    }

    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2nine_proof() {
        val path: Array<Any> = arrayOf(3,1)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf7WithSubTree(gtvPath)

        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)
        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expectet7and3ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))
    }

    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2three() {
        val path: Array<Any> = arrayOf(2)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf7WithSubTree(gtvPath)

        // How to calculate the hash of the sub tree?
        // 07 + [
        //        (00 + [(01 + [01]) + (01 + [09])] )
        //         +
        //        (01 + [03])
        //      ] ->
        // 07 + [
        //        (00 + [0102 + 010A] )
        //         +
        //        (01 + [03])
        //      ] ->
        // 07 + [
        //        (00 + 0203020B)
        //         +
        //        (01 + 04)
        //      ] ->
        // 07 + [ 000203020B0104 ] ->
        // 07 +   010304030C0205
        val expectedTree =
                "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       0001030803090209       \n" +
                "  / \\           \n" +
                " /   \\          \n" +
                " 0002030204   +   .   .   \n" +
                "    / \\         \n" +
                "- - *3 07010304030C0205 - - - - "


        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())
    }

    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2three_proof() {
        val path: Array<Any> = arrayOf(2)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf7WithSubTree(gtvPath)

        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)
        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expectet7and3ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))
    }

    // ---------------------
    // 4. Later we test to build a proof where the value-to-be-proved is a complex type (another array)
    // ---------------------

    /**
     * Note: This test depend on the auto-generated output of toString() of the "data class" of theGtv array.
     */
    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2subArray() {
        val path: Array<Any> = arrayOf(3)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf7WithSubTree(gtvPath)

        val expectedTree ="       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       0001030803090209       \n" +
                "  / \\           \n" +
                " /   \\          \n" +
                " 0002030204   +   .   .   \n" +
                "    / \\         \n" +
                "- - 0104 *GtvArray(array=[GtvInteger(integer=1), GtvInteger(integer=9), GtvInteger(integer=3)]) - - - - "


        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Proof -> Serialize
        val serialize: GtvArray = merkleProofTree.serializeToGtv()
        println("Serilalized: $serialize")

        val expectedSerialization = "GtvArray(array=[\n" +
                "  GtvInteger(integer=103), \n" + // 103 = array head node type
                "  GtvInteger(integer=7), \n" + // length of array
                "  GtvArray(array=[\n" +
                "    GtvInteger(integer=102), \n" + // 102 = dummy node
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=100), \n" + // 100 = hash
                "      GtvByteArray(bytearray=[0, 2, 3, 2, 4])\n" +
                "    ]),\n" +
                "    GtvArray(array=[\n" +
                "      GtvInteger(integer=102), \n" + // 102 = dummy node
                "      GtvArray(array=[\n" +
                "        GtvInteger(integer=100), \n" + // 100 = hash
                "        GtvByteArray(bytearray=[1, 4])\n" +
                "      ]), \n" +
                "      GtvArray(array=[\n" +
                "        GtvInteger(integer=101), \n" + // 101 = value to prove
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
                "    GtvByteArray(bytearray=[0, 1, 3, 8, 3, 9, 2, 9])\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(stripWhite(expectedSerialization), stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = factory.deserialize(serialize)


        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())

    }

    @Test
    fun test_ArrayLength7_withInnerLength3Array_path2subArray_proof() {
        val path: Array<Any> = arrayOf(3)
        val gtvPath:GtvPath =GtvPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = ArrayToGtvBinaryTreeHelper.buildTreeOf7WithSubTree(gtvPath)

        val merkleProofTree:GtvMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)
        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expectet7and3ElementArrayMerkleRoot, TreeHelper.convertToHex(merkleProofRoot))
    }
}