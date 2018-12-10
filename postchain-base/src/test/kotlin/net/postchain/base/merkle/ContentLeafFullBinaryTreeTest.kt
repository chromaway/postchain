package net.postchain.base.merkle

import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.GTXValue
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentLeafFullBinaryTreeTest {


    @Test
    fun testIntArrayLength4() {
        val intArray = intArrayOf(1,2,3,4)
        val expectedResult =
                "   +       \n" +
                "  / \\   \n" +
                " /   \\  \n" +
                " +   +   \n" +
                "/ \\ / \\ \n" +
                "1 2 3 4 \n"

        val intArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(intArrayList)

        val printer = BTreePrinter()
        val treePrintout = printer.printNode(fullBinaryTree)
        //println(treePrintout)

        assertEquals(expectedResult.trim(), treePrintout.trim())
    }

    @Test
    fun testIntArrayLength7() {
        val intArray = intArrayOf(1,2,3,4,5,6,7)
        val expectedResult =
                "       +               \n" +
                "      / \\       \n" +
                "     /   \\      \n" +
                "    /     \\     \n" +
                "   /       \\    \n" +
                "   +       +       \n" +
                "  / \\     / \\   \n" +
                " /   \\   /   \\  \n" +
                " +   +   +   7   \n" +
                "/ \\ / \\ / \\     \n" +
                "1 2 3 4 5 6 "

        val intArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(intArrayList)

        val printer = BTreePrinter()
        val treePrintout = printer.printNode(fullBinaryTree)
        //println(treePrintout)

        assertEquals(expectedResult.trim(), treePrintout.trim())
    }

    @Test
    fun testIntArrayLength9() {
        val intArray = intArrayOf(1,2,3,4,5,6,7,8,9)
        val expectedResult =
                "                +                               \n" +
                "              / \\               \n" +
                "             /   \\              \n" +
                "            /     \\             \n" +
                "           /       \\            \n" +
                "          /         \\           \n" +
                "         /           \\          \n" +
                "        /             \\         \n" +
                "       /               \\        \n" +
                "       +               9               \n" +
                "      / \\                       \n" +
                "     /   \\                      \n" +
                "    /     \\                     \n" +
                "   /       \\                    \n" +
                "   +       +       \n" +
                "  / \\     / \\   \n" +
                " /   \\   /   \\  \n" +
                " +   +   +   +   \n" +
                "/ \\ / \\ / \\ / \\ \n" +
                "1 2 3 4 5 6 7 8 \n"

        val intArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(intArrayList)

        val printer = BTreePrinter()
        val treePrintout = printer.printNode(fullBinaryTree)
        //println(treePrintout)

        assertEquals(expectedResult.trim(), treePrintout.trim())
    }


    @Test
    fun testIntArrayLength13() {
        val intArray = intArrayOf(1,2,3,4,5,6,7,8,9,0,1,2,3)
        val expectedResult =
                "              +                               \n" +
                "              / \\               \n" +
                "             /   \\              \n" +
                "            /     \\             \n" +
                "           /       \\            \n" +
                "          /         \\           \n" +
                "         /           \\          \n" +
                "        /             \\         \n" +
                "       /               \\        \n" +
                "       +               +               \n" +
                "      / \\             / \\       \n" +
                "     /   \\           /   \\      \n" +
                "    /     \\         /     \\     \n" +
                "   /       \\       /       \\    \n" +
                "   +       +       +       3       \n" +
                "  / \\     / \\     / \\           \n" +
                " /   \\   /   \\   /   \\          \n" +
                " +   +   +   +   +   +   \n" +
                "/ \\ / \\ / \\ / \\ / \\ / \\ \n" +
                "1 2 3 4 5 6 7 8 9 0 1 2 "

        val intArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(intArrayList)

        val printer = BTreePrinter()
        val treePrintout = printer.printNode(fullBinaryTree)
        //println(treePrintout)

        assertEquals(expectedResult.trim(), treePrintout.trim())
    }


    @Test
    fun testIntArrayLength7withInnerLength3Array() {
        val intArray = intArrayOf(1,2,3,4,5,6,7)
        val expectedResult =
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
                        "               +                               +                               \n" +
                        "              / \\                             / \\               \n" +
                        "             /   \\                           /   \\              \n" +
                        "            /     \\                         /     \\             \n" +
                        "           /       \\                       /       \\            \n" +
                        "          /         \\                     /         \\           \n" +
                        "         /           \\                   /           \\          \n" +
                        "        /             \\                 /             \\         \n" +
                        "       /               \\               /               \\        \n" +
                        "       +               +               +               7               \n" +
                        "      / \\             / \\             / \\                       \n" +
                        "     /   \\           /   \\           /   \\                      \n" +
                        "    /     \\         /     \\         /     \\                     \n" +
                        "   /       \\       /       \\       /       \\                    \n" +
                        "   1       2       3       +       5       6       \n" +
                        "                          / \\                   \n" +
                        "                         /   \\                  \n" +
                        "                         +   3   \n" +
                        "                        / \\                             \n" +
                        "                        1 2 "

        val intArrayList = TreeHelper.transformIntToGTXValue(intArray.toCollection(ArrayList()))

        // Add the inner ArrayGtxValue
        val innerIntArray = intArrayOf(1,2,3)
        val innerIntArrayList = TreeHelper.transformIntToGTXValue(innerIntArray.toCollection(ArrayList()))
        val innerGtxIntArray: Array<GTXValue> = innerIntArrayList.toTypedArray()
        val innerArrayGTXValue = ArrayGTXValue(innerGtxIntArray)
        intArrayList.set(3, innerArrayGTXValue)

        val fullBinaryTree: ContentLeafFullBinaryTree = CompleteBinaryTreeFactory.buildCompleteBinaryTree(intArrayList)

        val printer = BTreePrinter()
        val treePrintout = printer.printNode(fullBinaryTree)
        //println(treePrintout)

        assertEquals(expectedResult.trim(), treePrintout.trim())
    }


}