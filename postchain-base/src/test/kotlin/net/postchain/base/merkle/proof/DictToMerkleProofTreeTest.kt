package net.postchain.base.merkle.proof

import net.postchain.base.merkle.*
import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.GTXPath
import net.postchain.gtx.GTXPathFactory
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
 * In this class we test if we can generate proofs out of GTX dictionary structures.
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
 * Note: The testing of the exact serialization format is not very important and can be removed. The only important
 * thing is that we keep testing after deserialization.
 */

class DictToMerkleProofTreeTest {

    val calculator = MerkleHashCalculatorDummy()
    val factory = GtxMerkleProofTreeFactory(calculator)

    val expectedMerkleRoot1 = "08027170670203"
    val expectedMerkleRoot4 = "080102046A737976040802047372690405010204786C76696904070204787B730406"
    val expectedMerkleRootDictInDict = "0802717067090204696D6B6C78040C020477697A6972040B"


    // ---------------------
    // 1. First we test to build a proof where the value-to-be-proved a primitive type value in the dict.
    // ---------------------
    @Test
    fun test_tree_from_1dict() {
        val path: Array<Any> = arrayOf("one")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildThreeOf1_fromDict(gtxPath)

        // How to convert one to hash?:
        // "one" ->(serialization) 6F6E65
        // 01 + [6F6E65] ->
        // 01706F66 ->
        val expectedTree =" +   \n" +
                "/ \\ \n" +
                "01706F66 *1 "

        val merkleProofTree: GtxMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Proof -> Serialize
        val serialize: ArrayGTXValue = merkleProofTree.serializeToGtx()
        //println("Serilalized: $serialize")

        val expectedSerialization = "ArrayGTXValue(array=[\n" +
                "  IntegerGTXValue(integer=4),\n" + // 4 = dict head node type
                "  IntegerGTXValue(integer=1),\n" + // length of dict
                "  ArrayGTXValue(array=[\n" +
                "    IntegerGTXValue(integer=0),\n" + // 0 = hash
                "    ByteArrayGTXValue(bytearray=[1, 112, 111, 102])\n" +
                "  ]),\n" +
                "  ArrayGTXValue(array=[\n" +
                "    IntegerGTXValue(integer=1),  \n" + // 1 = value to prove
                "    IntegerGTXValue(integer=1)\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(TreeHelper.stripWhite(expectedSerialization), TreeHelper.stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = factory.deserialize(serialize)

        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        //println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())

    }


    @Test
    fun test_tree_from_1dict_proof() {
        val path: Array<Any> = arrayOf("one")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildThreeOf1_fromDict(gtxPath)
        // 08 + [01706F66 + (01 + [01])
        // 08 + [01706F66 + 0102]
        // 08 + 02717067 + 0203

        val merkleProofTree: GtxMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)


        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expectedMerkleRoot1, TreeHelper.convertToHex(merkleProofRoot))

    }

    @Test
    fun test_tree_from_4dict() {
        val path: Array<Any> = arrayOf("four")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildThreeOf4_fromDict(gtxPath)

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

        val merkleProofTree: GtxMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Proof -> Serialize
        val serialize: ArrayGTXValue = merkleProofTree.serializeToGtx()
        //println("Serilalized: $serialize")

        val expectedSerialization = "ArrayGTXValue(array=[\n" +
                "  IntegerGTXValue(integer=4), \n" + // 4 = dict head node type
                "  IntegerGTXValue(integer=4), \n" + // length of the dict
                "  ArrayGTXValue(array=[\n" +
                "    IntegerGTXValue(integer=2), \n" + // 2 = dummy node
                "    ArrayGTXValue(array=[\n" +
                "      IntegerGTXValue(integer=2), \n" + // 2 = dummy node
                "      ArrayGTXValue(array=[\n" +
                "        IntegerGTXValue(integer=0), \n" + // 0 = hash
                "        ByteArrayGTXValue(bytearray=[1, 103, 112, 118, 115])\n" +
                "      ]),\n" +
                "      ArrayGTXValue(array=[\n" +
                "        IntegerGTXValue(integer=1),   \n" + // 1 = value to prove
                "        IntegerGTXValue(integer=4)\n" +
                "      ])\n" +
                "    ]),  \n" +
                "    ArrayGTXValue(array=[\n" +
                "      IntegerGTXValue(integer=0),   \n" + // 0 = hash
                "      ByteArrayGTXValue(bytearray=[0, 2, 113, 112, 103, 2, 3])\n" +
                "    ])\n" +
                "  ]),   \n" +
                "  ArrayGTXValue(array=[\n" +
                "    IntegerGTXValue(integer=0),   \n" + // 0 = hash
                "    ByteArrayGTXValue(bytearray=[0, 1, 3, 119, 107, 117, 104, 104, 3, 6, 1, 3, 119, 122, 114, 3, 5])\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(TreeHelper.stripWhite(expectedSerialization), TreeHelper.stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = factory.deserialize(serialize)

        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        //println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())

    }

    @Test
    fun test_tree_from_4dict_proof() {
        val path: Array<Any> = arrayOf("four")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildThreeOf4_fromDict(gtxPath)

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

        val merkleProofTree: GtxMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)


        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expectedMerkleRoot4, TreeHelper.convertToHex(merkleProofRoot))

    }

    // ---------------------
    // 2. Then we test to build a proof where the value-to-be-proved is a primitive value located in a sub-dict.
    // ---------------------
    @Test
    fun test_tree_from_dictOfDict() {
        val path: Array<Any> = arrayOf("one", "seven")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildTreeOf1WithSubTree(gtxPath)

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

        val merkleProofTree: GtxMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Proof -> Serialize
        val serialize: ArrayGTXValue = merkleProofTree.serializeToGtx()
        println("Serilalized: $serialize")

        val expectedSerialization =  "ArrayGTXValue(array=[\n" +
                "  IntegerGTXValue(integer=4), \n" + // 4 = dict head node type
                "  IntegerGTXValue(integer=1), \n" + // length of the dict
                "  ArrayGTXValue(array=[\n" +
                "    IntegerGTXValue(integer=0),\n" +
                "    ByteArrayGTXValue(bytearray=[1, 112, 111, 102])\n" +
                "  ]),\n" +
                "  ArrayGTXValue(array=[\n" +
                "    IntegerGTXValue(integer=4), \n" + // 4 = dict head node type
                "    IntegerGTXValue(integer=2), \n" + // length of the dict
                "    ArrayGTXValue(array=[\n" +
                "      IntegerGTXValue(integer=0), \n" + // 0 = hash
                "      ByteArrayGTXValue(bytearray=[0, 2, 103, 107, 105, 106, 118, 2, 10])\n" +
                "    ]), \n" +
                "    ArrayGTXValue(array=[\n" +
                "      IntegerGTXValue(integer=2), \n" + // 2 = dummy node
                "      ArrayGTXValue(array=[\n" +
                "        IntegerGTXValue(integer=0), \n" + // 0 = hash
                "        ByteArrayGTXValue(bytearray=[1, 116, 102, 119, 102, 111])\n" +
                "      ]), \n" +
                "      ArrayGTXValue(array=[\n" +
                "        IntegerGTXValue(integer=1), \n" + // 1 = value to prove
                "        IntegerGTXValue(integer=7)\n" +
                "      ])\n" +
                "    ])\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(TreeHelper.stripWhite(expectedSerialization), TreeHelper.stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = factory.deserialize(serialize)

        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())

    }

    @Test
    fun test_tree_from_dictOfDict_proof() {
        val path: Array<Any> = arrayOf("one", "seven")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildTreeOf1WithSubTree(gtxPath)

        val merkleProofTree: GtxMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expectedMerkleRootDictInDict, TreeHelper.convertToHex(merkleProofRoot))
    }

    // ---------------------
    // 3. Later we test to build a proof where the value-to-be-proved is a complex type (another dict)
    // ---------------------
    /**
     * This test will create a proof of a sub-dictionary inside the main dictionary.
     *
     * Note: This test depend on the auto-generated output of toString() of the "data class" of the GTX Dict.
     */
    @Test
    fun test_tree_from_dict_of_dict_where_path_is_to_sub_dict() {
        val path: Array<Any> = arrayOf("one")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildTreeOf1WithSubTree(gtxPath)

        val expectedTree = " +   \n" +
                "/ \\ \n" +
                "01706F66 *DictGTXValue(dict={seven=IntegerGTXValue(integer=7), eight=IntegerGTXValue(integer=8)}) "

        val merkleProofTree: GtxMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        // Print the result tree
        val printer = TreePrinter()
        val pbt = PrintableTreeFactory.buildPrintableTreeFromProofTree(merkleProofTree)
        val resultPrintout = printer.printNode(pbt)
        //println(resultPrintout)

        Assert.assertEquals(expectedTree.trim(), resultPrintout.trim())

        // Proof -> Serialize
        val serialize: ArrayGTXValue = merkleProofTree.serializeToGtx()
        //println("Serilalized: $serialize")

        val expectedSerialization = "ArrayGTXValue(array=[\n" +
                "  IntegerGTXValue(integer=4),\n" +  // 4 = dict head node type
                "  IntegerGTXValue(integer=1),\n" + // lenght of the dict
                "  ArrayGTXValue(array=[\n" +
                "    IntegerGTXValue(integer=0),\n" + // 0 = Hash
                "    ByteArrayGTXValue(bytearray=[1, 112, 111, 102])\n" +
                "  ]),\n" +
                "  ArrayGTXValue(array=[\n" +
                "    IntegerGTXValue(integer=1), \n" + // 1 = value to be proved (in this case an entire dict)
                "    DictGTXValue(dict={\n" +  // The value is a DictGTXValue, in it's raw form
                "      seven=IntegerGTXValue(integer=7), \n" +
                "      eight=IntegerGTXValue(integer=8)\n" +
                "    })\n" +
                "  ])\n" +
                "])\n"

        Assert.assertEquals(TreeHelper.stripWhite(expectedSerialization), TreeHelper.stripWhite(serialize.toString())) // Not really needed, Can be removed

        // Serialize -> deserialize
        val deserialized = factory.deserialize(serialize)

        // Print the result tree
        val pbtDes = PrintableTreeFactory.buildPrintableTreeFromProofTree(deserialized)
        val deserializedPrintout = printer.printNode(pbtDes)
        //println(deserializedPrintout)

        Assert.assertEquals(expectedTree.trim(), deserializedPrintout.trim())


    }

    @Test
    fun test_tree_from_dict_of_dict_where_path_is_to_sub_dict_proof() {
        val path: Array<Any> = arrayOf("one")
        val gtxPath: GTXPath = GTXPathFactory.buildFromArrayOfPointers(path)
        val treeHolder = DictToGtxBinaryTreeHelper.buildTreeOf1WithSubTree(gtxPath)

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

        val merkleProofTree: GtxMerkleProofTree = factory.buildFromBinaryTree(treeHolder.clfbTree)

        val merkleProofRoot = merkleProofTree.calculateMerkleRoot(calculator)
        assertEquals(expectedMerkleRootDictInDict, TreeHelper.convertToHex(merkleProofRoot))
    }


}