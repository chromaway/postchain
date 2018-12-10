package net.postchain.base.merkle

import mu.KLogging
import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.GTXValue
import net.postchain.gtx.IntegerGTXValue
import kotlin.math.pow


/**
 * This is a "full" binary tree that stores values in the leafs only.
 *
 * (A "full" binary tree is a binary tree where each node has 2 or 0 children.
 * In a Full Binary, number of leaf nodes is number of internal nodes plus 1
 *   L = I + 1)
 *
 * The tree is filled from left to right.
 *
 *  Our rule for transforming an array into a [ContentLeafFullBinaryTree] is illustrated by Example3 below:
 *  -------------
 *  Example3:
 *
 *  [1 2 3 4 5 6 7]
 *  -------------
 *  As the name suggests, we have values/content in the leafs (but no content in the nodes):
 *  -------------
 *                 root
 *             /          \
 *        node1234        node567
 *       /     \           /     \
 *   node12  node34      node56   7
 *   /  \     /  \       /   \
 *  1   2    3   4      5     6
 *  -------------
 *
 *  NOTE:
 *  These trees will typically not be "balanced" nor "complete" (since we will transform arrays of arrays into trees)
 *
 */

/**
 * Fbt = Full Binary Tree
 */
sealed class FbtElement

/**
 *  Doesn't hold a [GTXValue]
 */
data class Node(val left: FbtElement, val right: FbtElement): FbtElement()

/**
 *  Holds a [GTXValue]
 */
data class Leaf(val content: GTXValue): FbtElement()


/**
 * Wrapper class for the root object.
 * ("content leaf" is supposed to indicate that it's the Leaf that carries all the content of the tree)
 */
class ContentLeafFullBinaryTree(val root: FbtElement) {

}

/**
 * The factory does the actual conversion between list and tree.
 */
object CompleteBinaryTreeFactory : KLogging() {

    /**
     * Builds a [ContentLeafFullBinaryTree]
     *
     * @param originalList A collection of [GTXValue] used to create the tree
     */
    fun buildCompleteBinaryTree(originalList: List<GTXValue>): ContentLeafFullBinaryTree {
        val result = buildSubTree(originalList)
        return ContentLeafFullBinaryTree(result)
    }

   /**
     * Builds a [ContentLeafFullBinaryTree]
     *
     * @param arrayGTXValue An [ArrayGTXValue] holding the components needed to build the tree
     */
    fun buildCompleteBinaryTree(arrayGTXValue: ArrayGTXValue): ContentLeafFullBinaryTree {
        val result = buildFromArrayGTXValue(arrayGTXValue)
        return ContentLeafFullBinaryTree(result)
    }

    // --------------- Private funcs ---------

    private fun buildFromArrayGTXValue(arrayGTXValue: ArrayGTXValue): FbtElement {
        val ret: List<GTXValue> = arrayGTXValue.array.map {it}
        return buildSubTree(ret)
    }


    /**
     * Builds the (sub?)tree from a list. We do this is in parts:
     *
     * 1. Transform each [GTXValue] in the list into a [Leaf]
     *    If a [GTXValue] proves to be a recursive type, make it into a [FbtElement]
     * 2. Create the nodes that exist above the leaf, all the way to the root.
     *
     * @return Root [FbtElement] node
     */
    private fun buildSubTree(inList: List<GTXValue>): FbtElement {
        val leafArray = arrayListOf<FbtElement>()

        // 1. Build first (leaf) layer
        for (leaf: GTXValue in inList) {
            val locbtElement = when (leaf) {
                is ArrayGTXValue -> buildFromArrayGTXValue(leaf)
                else -> Leaf(leaf)
            }
            leafArray.add(locbtElement)
        }

        // 2. Build all higher layers
        val result = buildHigherLayer(1, leafArray)

        return result.get(0)
    }

    /**
     * Calls itself until the return value only holds 1 element
     *
     * @param layer What layer we aim calculate
     * @param inList The array of nodes we should build from
     * @return All [FbtElement] nodes of the next layer
     */
    private fun buildHigherLayer(layer: Int, inList: List<FbtElement>): List<FbtElement> {

        if (inList.isEmpty()) {
            throw IllegalStateException("Cannot work on empty arrays. Layer: $layer")
        } else if (inList.size == 1) {
            return inList
        }

        val returnArray = arrayListOf<FbtElement>()
        var nrOfNodesToCreate = inList.size / 2
        var leftValue: FbtElement? = null
        var isLeft = true
        for (element: FbtElement in inList) {
            if(isLeft)  {
                leftValue = element
                isLeft = false
            } else {
                val tempNode = Node(leftValue!!, element)
                returnArray.add(tempNode)
                nrOfNodesToCreate--
                isLeft = true
                leftValue = null
            }
        }

        if (!isLeft) {
            // If there is odd number of nodes, then move the last node up one level
            returnArray.add(leftValue!!)
        }

        // Extra check
        if (nrOfNodesToCreate != 0) {
            logger.warn("Why didn't we build exactly the correct amount? Layer: $layer , residue: $nrOfNodesToCreate , input array size: ${inList.size}")
        }

        return buildHigherLayer((layer + 1), returnArray)

    }

}

/**
 * Utility class to turn binary trees into readable strings
 * (can be used for debugging)
 * Source for this code (with some mods): https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram
 */
class BTreePrinter {

    var buf: StringBuffer = StringBuffer()

    fun printNode(tree: ContentLeafFullBinaryTree): String {
        buf = StringBuffer()
        val root = tree.root
        val maxLevel: Int = maxLevel(root)

        val tmpList = arrayListOf(root)
        printNodeInternal(tmpList, 1, maxLevel, 0)
        return buf.toString()
    }

    private fun printNodeInternal(nodes: ArrayList<FbtElement>, level: Int, maxLevel: Int, compensateFirstSpaces: Int) {
        if (nodes.isEmpty())
            return

        val floor: Int = maxLevel - level
        val numberTwo = 2.0
        val endgeLines: Int = (numberTwo.pow(maxOf(floor-1, 0))).toInt()
        val firstSpaces: Int = (numberTwo.pow(floor) - 1 + compensateFirstSpaces).toInt()
        val betweenSpaces: Int = (numberTwo.pow(floor+1) - 1).toInt()

        // Debugging, probably won't need it
        println("nodes.size ${nodes.size}, level: $level , floor: $floor , endgeLines: $endgeLines , betweenSpaces: $betweenSpaces , firstSpaces: $firstSpaces , compenstation: $compensateFirstSpaces")

        printWhitespaces(firstSpaces)

        var compensateForEmptNodes = compensateFirstSpaces
        var leafCount = 0

        val newNodes = arrayListOf<FbtElement>()
        for (node in nodes) {
            when (node) {
                is Node -> {
                    buf.append("+") // No data to print // print(node.data)
                    newNodes.add(node.left)
                    newNodes.add(node.right)
                    compensateForEmptNodes += leafCount * (betweenSpaces + 1)
                }
                is Leaf -> {
                    leafCount++
                    val content = node.content
                    val dataStr = when (content) {
                        is IntegerGTXValue -> content.integer.toString()
                        else ->  content.toString()
                    }
                    buf.append(dataStr)
                }
            }

            printWhitespaces(betweenSpaces)
        }
        buf.appendln("")

        for (i in 1..endgeLines) {
            for (j in 0..(nodes.size - 1)) {
                printWhitespaces(firstSpaces - i)
                val tmpNode = nodes.get(j)

                when(tmpNode) {
                    is Node -> {
                        buf.append("/")
                        printWhitespaces(i + i -1)
                        buf.append("\\")
                        printWhitespaces(endgeLines + endgeLines - i)
                    }
                    is Leaf -> {
                        printWhitespaces(i + 1)
                        printWhitespaces(endgeLines + endgeLines)
                    }
                }
            }

            buf.appendln("")
        }

        printNodeInternal(newNodes, (level + 1), maxLevel, compensateForEmptNodes)
    }

    private fun printWhitespaces(count: Int) {
        for (i in 0..(count-1)) {
            buf.append(" ")
        }
    }

    private fun maxLevel(node: FbtElement): Int {
        return when (node) {
            is Leaf -> 1
            is Node -> maxOf(maxLevel(node.left), maxLevel(node.right)) + 1
        }
    }

}


