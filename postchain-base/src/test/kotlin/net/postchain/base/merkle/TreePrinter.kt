package net.postchain.base.merkle

import net.postchain.gtx.GTXValue
import net.postchain.gtx.IntegerGTXValue
import kotlin.math.pow


/**
 * The purpose of these classes ([PrintableBinaryTree] and [PTreeElement] etc) is to be able to
 * visualize different types of trees and merkle pathes.
 *
 * (It turned out that the easiest way was to populate a tree with empty nodes
 * so we always draw a "complete" full binary tree, even if the source tree
 * lacks parts.)
 */
open class PTreeElement

class PNode(val left: PTreeElement, val right: PTreeElement): PTreeElement()
/**
 * [PContentNode] represents a hanging leaf that is not at max level depth
 */
class PContentNode(val content: String, val left: PTreeElement, val right: PTreeElement): PTreeElement()
class PLeaf(val content: String): PTreeElement()

/**
 * We use empty elements to make it easier to draw non existing parts of the tree
 */
open class PEmptyElement: PTreeElement()
class PEmptyLeaf: PEmptyElement()
class PEmptyNode(val left: PEmptyElement, val right: PEmptyElement): PEmptyElement()


/**
 * Wrapper class for the root object.
 * ("content leaf" is supposed to indicate that it's the PLeaf that carries all the content of the tree)
 */
class PrintableBinaryTree(val root: PTreeElement) {

}


/**
 * Transforms other trees to printable trees
 */
object PrintableTreeFactory {

    fun buildPrintableTreeFromClfbTree(tree: ContentLeafFullBinaryTree): PrintableBinaryTree {
        val maxLevel = tree.maxLevel()
        println("Max level: $maxLevel")
        val newRoot: PTreeElement = fromClfbTreeInternal(1, maxLevel, tree.root)
        return PrintableBinaryTree(newRoot)
    }

    fun buildPrintableTreeFromProofTree(tree: MerkleProofTree): PrintableBinaryTree {
        val maxLevel = tree.maxLevel()
        println("Max level: $maxLevel")
        val newRoot: PTreeElement = fromProofTreeInternal(1, maxLevel, tree.root)
        return PrintableBinaryTree(newRoot)

    }

    private fun createEmptyInternal(currentLevel: Int, maxLevel: Int): PEmptyElement {
        return if (currentLevel < maxLevel) {
            val left = createEmptyInternal(currentLevel + 1, maxLevel)
            val right = createEmptyInternal(currentLevel + 1, maxLevel)
            PEmptyNode(left, right)
        } else {
            PEmptyLeaf()
        }
    }

    private fun fromClfbTreeInternal(currentLevel: Int, maxLevel: Int, inElement: FbtElement): PTreeElement {
        return when(inElement) {
            is Leaf -> {
                if (currentLevel < maxLevel) {
                    // Create node instead of leaf
                    val content = convertGtxToString(inElement.content)
                    println("Early leaf $content at level: $currentLevel")
                    val emptyLeft: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    val emptyRight: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    PContentNode(content, emptyLeft, emptyRight)
                } else {
                    // Normal leaf
                    val content = convertGtxToString(inElement.content)
                    println("Normal leaf $content at level: $currentLevel")
                    PLeaf(content)
                }
            }
            is Node -> {
                val left = fromClfbTreeInternal(currentLevel + 1, maxLevel, inElement.left)
                val right = fromClfbTreeInternal(currentLevel + 1, maxLevel, inElement.right)
                PNode(left, right)
            }
        }
    }

    private fun fromProofTreeInternal(currentLevel: Int, maxLevel: Int, inElement: MerkleProofElement): PTreeElement {
        return when(inElement) {
            is ProofGtxLeaf -> {
                if (currentLevel < maxLevel) {
                    // Create node instead of leaf
                    val content = convertGtxToString(inElement.content)
                    println("Early leaf $content at level: $currentLevel")
                    val emptyLeft: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    val emptyRight: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    PContentNode(content, emptyLeft, emptyRight)
                } else {
                    // Normal leaf
                    val content = convertGtxToString(inElement.content)
                    println("Normal leaf $content at level: $currentLevel")
                    PLeaf(content)
                }
            }
            is ProofHashedLeaf -> {
                if (currentLevel < maxLevel) {
                    // Create node instead of leaf
                    val content = TreeHelper.convertToHex(inElement.hash)
                    //println("Early hash leaf $content at level: $currentLevel")
                    val emptyLeft: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    val emptyRight: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    PContentNode(content, emptyLeft, emptyRight)
                } else {
                    // Normal leaf
                    val content = TreeHelper.convertToHex(inElement.hash)
                    //println("Normal hash leaf $content at level: $currentLevel")
                    PLeaf(content)
                }
            }
            is ProofNode -> {
                val left = fromProofTreeInternal(currentLevel + 1, maxLevel, inElement.left)
                val right = fromProofTreeInternal(currentLevel + 1, maxLevel, inElement.right)
                PNode(left, right)
            }
        }
    }

    private fun convertGtxToString(gtx: GTXValue): String {
        val dataStr = when (gtx) {
            is IntegerGTXValue -> gtx.integer.toString()
            else ->  gtx.toString()
        }
        return dataStr
    }


}

/**
 * Utility class to turn binary trees into readable strings
 * (can be used for debugging)
 * Source for this code (with some mods): https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram
 */
class TreePrinter {

    var buf: StringBuffer = StringBuffer()

    fun printNode(treePrintable: PrintableBinaryTree): String {
        buf = StringBuffer()
        val root = treePrintable.root
        val maxLevel: Int = maxLevel(root)

        val tmpList = arrayListOf(root)
        printNodeInternal(tmpList, 1, maxLevel, 0)
        return buf.toString()
    }

    private fun printNodeInternal(nodes: ArrayList<PTreeElement>, level: Int, maxLevel: Int, compensateFirstSpaces: Int) {
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

        val newNodes = arrayListOf<PTreeElement>()
        for (node in nodes) {
            when (node) {
                is PNode -> {
                    buf.append("+") // No data to print // print(node.data)
                    newNodes.add(node.left)
                    newNodes.add(node.right)
                    compensateForEmptNodes += leafCount * (betweenSpaces + 1)
                }
                is PContentNode -> {
                    buf.append(node.content)
                    newNodes.add(node.left)
                    newNodes.add(node.right)
                    compensateForEmptNodes += leafCount * (betweenSpaces + 1)
                }
                is PEmptyNode -> {
                    buf.append(".") // No data to print // print(node.data)
                    newNodes.add(node.left)
                    newNodes.add(node.right)
                    compensateForEmptNodes += leafCount * (betweenSpaces + 1)
                }
                is PLeaf -> {
                    leafCount++
                    buf.append(node.content)
                }
                is PEmptyLeaf -> {
                    leafCount++
                    buf.append("-")
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
                    is PNode -> {
                        buf.append("/")
                        printWhitespaces(i + i -1)
                        buf.append("\\")
                        printWhitespaces(endgeLines + endgeLines - i)
                    }
                    is PContentNode -> {
                        printWhitespaces(i + i + 1)
                        printWhitespaces(endgeLines + endgeLines - i)
                    }
                    is PEmptyNode -> {
                        printWhitespaces(i + i + 1)
                        printWhitespaces(endgeLines + endgeLines - i)
                    }
                    is PLeaf -> {
                        printWhitespaces(i + 1)
                        printWhitespaces(endgeLines + endgeLines)
                    }
                    is PEmptyLeaf -> {
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

    private fun maxLevel(node: PTreeElement): Int {
        return when (node) {
            is PLeaf -> 1
            is PNode -> maxOf(maxLevel(node.left), maxLevel(node.right)) + 1
            else -> 0
        }
    }

}

