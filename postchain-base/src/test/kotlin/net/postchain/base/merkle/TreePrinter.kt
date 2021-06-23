// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.merkle

import net.postchain.base.merkle.proof.MerkleProofElement
import net.postchain.base.merkle.proof.ProofHashedLeaf
import net.postchain.base.merkle.proof.ProofNode
import net.postchain.base.merkle.proof.ProofValueLeaf
import net.postchain.gtv.*
import net.postchain.gtv.merkle.GtvBinaryTree
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree
import net.postchain.gtv.merkle.proof.ProofNodeGtvArrayHead
import net.postchain.gtv.merkle.proof.ProofNodeGtvDictHead
import kotlin.math.pow
import net.postchain.common.data.Hash


/**
 * The purpose of these classes ([PrintableBinaryTree] and [PTreeElement] etc) is to be able to
 * visualize different types of trees and merkle pathes.
 *
 * (It turned out that the easiest way was to populate a tree with empty nodes
 * so we always draw a "complete" full binary tree, even if the source tree
 * lacks parts.)
 */
open class PTreeElement

class PNode(val left: PTreeElement, val right: PTreeElement, val pathLeaf: Boolean, val isPath: Boolean): PTreeElement()
/**
 * [PContentNode] represents a hanging leaf that is not at max level depth
 */
class PContentNode(val content: String, val left: PTreeElement, val right: PTreeElement, val pathLeaf: Boolean, isPath: Boolean): PTreeElement()
class PLeaf(val content: String, val pathLeaf: Boolean): PTreeElement()

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
class PrintableBinaryTree(val root: PTreeElement)



fun convertGtxToString(gtx: Gtv): String {
    return when (gtx) {
        is GtvNull -> "N/A"
        is GtvInteger -> gtx.integer.toString()
        is GtvString -> gtx.asString()
        is GtvByteArray -> TreeHelper.convertToHex(gtx.asByteArray())

        else ->  gtx.toString()
    }
}

fun convertHashToString(hash: Hash): String {
    return TreeHelper.convertToHex(hash)
}

/**
 * Transforms other trees to printable trees
 */
object PrintableTreeFactory {

    fun buildPrintableTreeFromClfbTree(tree: GtvBinaryTree): PrintableBinaryTree {
        val maxLevel = tree.maxLevel()
        //println("Max level: $maxLevel")
        val newRoot: PTreeElement = genericTreeInternal(1, maxLevel, tree.root, ::convertGtxToString)
        return PrintableBinaryTree(newRoot)
    }

    fun buildPrintableTreeFromProofTree(tree: GtvMerkleProofTree): PrintableBinaryTree {
        val maxLevel = tree.maxLevel()
        //println("Max level: $maxLevel")
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

    @Suppress("UNCHECKED_CAST")
    private fun <T>genericTreeInternal(currentLevel: Int, maxLevel: Int, inElement: BinaryTreeElement, toStr: (T) -> String): PTreeElement {
        return when(inElement) {
            is EmptyLeaf -> PEmptyLeaf()
            is Leaf<*> -> {
                if (currentLevel < maxLevel) {
                    // Create node instead of leaf
                    val content: String = toStr(inElement.content as T)
                    //val content = convertGtxToString(inElement.content as Gtv)
                    //println("Early leaf $content at level: $currentLevel")
                    val emptyLeft: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    val emptyRight: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    PContentNode(content, emptyLeft, emptyRight, inElement.isPathLeaf(), inElement.isPath())
                } else {
                    // Normal leaf
                    val content = toStr(inElement.content as T)
                    //println("Normal leaf $content at level: $currentLevel")
                    PLeaf(content, inElement.isPathLeaf())
                }
            }
            /*
            // We no longer check the cache during Binary Tree construction
            is CachedLeaf -> {
                if (currentLevel < maxLevel) {
                    // Create node instead of leaf
                    val content: String = "(" + inElement.cachedSummary.merkleHash.toHex() + ")"
                    //println("Early cached leaf $content at level: $currentLevel")
                    val emptyLeft: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    val emptyRight: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    PContentNode(content, emptyLeft, emptyRight, inElement.isPathLeaf(), inElement.isPath())
                } else {
                    // Normal leaf
                    val content: String = "(" + inElement.cachedSummary.merkleHash.toHex() + ")"
                    //println("Normal cached leaf $content at level: $currentLevel")
                    PLeaf(content, inElement.isPathLeaf())
                }
            }
             */
            is Node -> {
                val left = genericTreeInternal(currentLevel + 1, maxLevel, inElement.left, toStr)
                val right = genericTreeInternal(currentLevel + 1, maxLevel, inElement.right, toStr)
                PNode(left, right, inElement.isPathLeaf(), inElement.isPath())
            }
            else -> throw IllegalStateException("Not handling this $inElement")
        }
    }

    private fun fromProofTreeInternal(currentLevel: Int, maxLevel: Int, inElement: MerkleProofElement): PTreeElement {
        return when(inElement) {
            is ProofValueLeaf<*> -> {
                if (currentLevel < maxLevel) {
                    // Create node instead of leaf
                    val content = convertGtxToString(inElement.content as Gtv)
                    //println("Early leaf $content at level: $currentLevel")
                    val emptyLeft: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    val emptyRight: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    PContentNode(content, emptyLeft, emptyRight, true, true)
                } else {
                    // Normal leaf
                    val content = convertGtxToString(inElement.content as Gtv)
                    //println("Normal leaf $content at level: $currentLevel")
                    PLeaf(content, true)
                }
            }
            is ProofHashedLeaf -> {
                if (currentLevel < maxLevel) {
                    // Create node instead of leaf
                    val content = TreeHelper.convertToHex(inElement.merkleHash)
                    //println("Early hash leaf $content at level: $currentLevel")
                    val emptyLeft: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    val emptyRight: PEmptyElement = createEmptyInternal(currentLevel + 1, maxLevel)
                    PContentNode(content, emptyLeft, emptyRight, false, false)
                } else {
                    // Normal leaf
                    val content = TreeHelper.convertToHex(inElement.merkleHash)
                    //println("Normal hash leaf $content at level: $currentLevel")
                    PLeaf(content, false)
                }
            }
            is ProofNode -> {
                val left = fromProofTreeInternal(currentLevel + 1, maxLevel, inElement.left)
                val right = fromProofTreeInternal(currentLevel + 1, maxLevel, inElement.right)
                val isPath = when (inElement) {
                    is ProofNodeGtvDictHead -> {
                        inElement.pathElem != null
                    }
                    is ProofNodeGtvArrayHead -> {
                        inElement.pathElem != null
                    }
                    else -> {
                        false
                    }
                }
                PNode(left, right, false, isPath)
            }
            else -> throw IllegalStateException("Should have handled this element type: $inElement")
        }
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
        //println("begin -----------------")
        buf = StringBuffer()
        val root = treePrintable.root
        val maxLevel: Int = maxLevel(root)

        val tmpList = arrayListOf(root)
        printNodeInternal(tmpList, 1, maxLevel, 0)
        //println("end -----------------")
        return buf.toString()
    }

    private fun printNodeInternal(nodes: ArrayList<PTreeElement>, level: Int, maxLevel: Int, compensateFirstSpaces: Int) {
        //println("Internal -----------------")
        if (nodes.isEmpty())
            return

        val floor: Int = maxLevel - level
        val numberTwo = 2.0
        val endgeLines: Int = (numberTwo.pow(maxOf(floor-1, 0))).toInt()
        val firstSpaces: Int = (numberTwo.pow(floor) - 1 + compensateFirstSpaces).toInt()
        val betweenSpaces: Int = (numberTwo.pow(floor+1) - 1).toInt()

        // Debugging, probably won't need it
        //println("nodes.size ${nodes.size}, level: $level , floor: $floor , endgeLines: $endgeLines , betweenSpaces: $betweenSpaces , firstSpaces: $firstSpaces , compenstation: $compensateFirstSpaces")

        printWhitespaces(firstSpaces)

        var compensateForEmptNodes = compensateFirstSpaces
        var leafCount = 0

        val newNodes = arrayListOf<PTreeElement>()
        for (node in nodes) {
            when (node) {
                is PNode -> {
                    if (node.pathLeaf) {
                        buf.append("#") // This is a proof leaf
                    } else if (node.isPath) {
                        buf.append("*") // This is a node part of a path
                    } else {
                        buf.append("+") // Non path node
                    }
                    newNodes.add(node.left)
                    newNodes.add(node.right)
                    compensateForEmptNodes += leafCount * (betweenSpaces + 1)
                }
                is PContentNode -> {
                    if (node.pathLeaf) {
                        buf.append("*" + node.content)
                    } else {
                        buf.append(node.content)
                    }
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
                    if (node.pathLeaf) {
                        buf.append("*" + node.content)
                    } else {
                        buf.append(node.content)
                    }
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
                //println("edgeLine: $i ,node: $j, firstpaces: $firstSpaces")
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

        printNodeInternal(newNodes, (level + 1), maxLevel, 0)
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

