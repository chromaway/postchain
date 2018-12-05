package net.postchain.base

import mu.KLogging
import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.GTXValue
import net.postchain.gtx.IntegerGTXValue
import kotlin.math.pow


/**
 * This is a complete binary tree that stores values in the leafs only.
 *
 * (A complete binary tree is a binary tree, which is completely filled, with the possible exception
 *     of the bottom level, which is filled from left to right.)
 *
 *  Our rule for transforming an array into a [ContentLeafCompleteBinaryTree] is illustrated by Example3 below:
 *  -------------
 *  Example3:
 *
 *  [1 2 3 4 5 6 7]
 *  -------------
 *  As the name suggests, we have values in the leafs, but with empty nodes:
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
 */

sealed class CbtElement // Cbt = CompleteBinaryTree

/**
 *  Doesn't hold a [GTXValue]
 */
data class Node(val left: CbtElement, val right: CbtElement): CbtElement()

/**
 *  Holds a [GTXValue]
 */
data class Leaf(val content: GTXValue): CbtElement()


/**
 * Wrapper class for the root object.
 * (The name of this structure means that it's the Leaf that carries all the content of the tree)
 */
class ContentLeafCompleteBinaryTree(val root: CbtElement) {

}

/**
 * The factory does the actual conversion between list and tree.
 */
object CompleteBinaryTreeFactory : KLogging() {

    /**
     * Builds a [ContentLeafCompleteBinaryTree]
     *
     * @param orginalList A collection of [GTXValue] used to create the tree
     */
    fun buildCompleteBinaryTree(orginalList: List<GTXValue>): ContentLeafCompleteBinaryTree {
        val result = buildSubTree(orginalList)
        return ContentLeafCompleteBinaryTree(result)
    }

   /**
     * Builds a [ContentLeafCompleteBinaryTree]
     *
     * @param arrayGTXValue An [ArrayGTXValue] holding the components needed to build the tree
     */
    fun buildCompleteBinaryTree(arrayGTXValue: ArrayGTXValue): ContentLeafCompleteBinaryTree {
        val result = buildFromArrayGTXValue(arrayGTXValue)
        return ContentLeafCompleteBinaryTree(result)
    }

    // --------------- Private funcs ---------

    private fun buildFromArrayGTXValue(arrayGTXValue: ArrayGTXValue): CbtElement {
        val ret: List<GTXValue> = arrayGTXValue.array.map {it}
        return buildSubTree(ret)
    }


    /**
     * Builds the (sub?)tree from a list. We do this is in parts:
     *
     * 1. Transform each [GTXValue] in the list into a [Leaf]
     *    If a [GTXValue] proves to be a recursive type, make it into a [CbtElement]
     * 2. Create the nodes that exist above the leaf, all the way to the root.
     *
     * @return Root [CbtElement] node
     */
    private fun buildSubTree(inList: List<GTXValue>): CbtElement {
        val leafArray = arrayListOf<CbtElement>()

        // 1. Build first (leaf) layer
        for (leaf: GTXValue in inList) {
            val locbtElement = when (leaf) {
                is ArrayGTXValue -> buildFromArrayGTXValue(leaf)
                else -> Leaf(leaf) }
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
     * @return All [CbtElement] nodes of the next layer
     */
    private fun buildHigherLayer(layer: Int, inList: List<CbtElement>): List<CbtElement> {

        if (inList.isEmpty()) {
            throw IllegalStateException("Cannot work on empty arrays. Layer: $layer")
        } else if (inList.size == 1) {
            return inList
        }

        val returnArray = arrayListOf<CbtElement>()
        var nrOfNodesToCreate = inList.size / 2
        var leftValue: CbtElement? = null
        var isLeft = true
        for (element: CbtElement in inList) {
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
 * Source: https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram
 */
class BTreePrinter {

    var buf: StringBuffer = StringBuffer()

    fun printNode(tree: ContentLeafCompleteBinaryTree): String {
        buf = StringBuffer()
        val root = tree.root
        val maxLevel: Int = maxLevel(root)

        val tmpList = arrayListOf(root)
        printNodeInternal(tmpList, 1, maxLevel, 0)
        return buf.toString()
    }

    private fun printNodeInternal(nodes: ArrayList<CbtElement>, level: Int, maxLevel: Int, compensateFirstSpaces: Int) {
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

        val newNodes = arrayListOf<CbtElement>()
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

    private fun maxLevel(node: CbtElement): Int {
        return when (node) {
            is Leaf -> 1
            is Node -> maxOf(maxLevel(node.left), maxLevel(node.right)) + 1
        }
    }

}


