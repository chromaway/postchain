package net.postchain.base.merkle

import mu.KLogging
import net.postchain.gtx.*
import java.util.TreeSet
import java.util.SortedSet


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
    /**
     * Mostly for debugging
     */
    fun maxLevel(): Int {
        return maxLevelInternal(root)
    }

    private fun maxLevelInternal(node: FbtElement): Int {
        return when (node) {
            is Leaf -> 1
            is Node -> maxOf(maxLevelInternal(node.left), maxLevelInternal(node.right)) + 1
        }
    }

}

class TreeElementFinder<T> {

    /**
     * Use this to find [GTXValue] s in the tree
     *
     * @param toFind this can be a string or int, depending on the type param T
     * @param node the root of the tree we are looking in
     * @return A list of [GTXValue] , usually just containing one element, but can contain many, if say the same
     *          string "foo" appear in many places in the tree.
     */
    fun findGtxValueFromPrimitiveType(toFind: T, node: FbtElement): List<GTXValue> {
        val retArr = arrayListOf<GTXValue>()
        when (node) {
            is Node ->  {
                val leftList = findGtxValueFromPrimitiveType(toFind, node.left)
                val rightList = findGtxValueFromPrimitiveType(toFind, node.right)
                retArr.addAll(leftList)
                retArr.addAll(rightList)
            }
            is Leaf -> {
                val gtxVal = node.content
                when (gtxVal) {
                    is StringGTXValue -> {
                        if (toFind is String && toFind == gtxVal.string) {
                            println("Found the string $toFind")
                            retArr.add(gtxVal)
                        }
                    }
                    is IntegerGTXValue -> {
                        //println("Looking for: $toFind a num: ${gtxVal.integer} ")
                        if (toFind is Int && toFind.toString() == gtxVal.integer.toString()) { // TODO: This conversion to string is ugly but for some reason comparison beween ints did not work!!??
                            println("Found the int $toFind")
                            retArr.add(gtxVal)
                        }
                    }
                }
            }
        }
        return retArr
    }
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

    /**
     * Builds a [ContentLeafFullBinaryTree]
     *
     * @param dictGTXValue An [DictGTXValue] holding the components needed to build the tree
     */
    fun buildCompleteBinaryTree(dictGTXValue: DictGTXValue): ContentLeafFullBinaryTree {
        val result = buildFromDictGTXValue(dictGTXValue)
        return ContentLeafFullBinaryTree(result)
    }

    // --------------- Private funcs ---------

    private fun buildFromArrayGTXValue(arrayGTXValue: ArrayGTXValue): FbtElement {
        val ret: List<GTXValue> = arrayGTXValue.array.map {it}
        return buildSubTree(ret)
    }

    /**
     * The strategy for transforming [DictGTXValue] is pretty simple, just flatten it into an array.
     * If you want to prove a ( [String] to [GTXValue] ) pair, you then have to prove both elements.
     */
    private fun buildFromDictGTXValue(dictGTXValue: DictGTXValue): FbtElement {
        val keys: SortedSet<String> = dictGTXValue.dict.keys.toSortedSet() // Needs to be sorted, or else the order is undefined
        val flattenedDictList = arrayListOf<GTXValue>()

        for (key in keys) {
            //println("key extracted: $key")
            val keyGtxString: GTXValue = StringGTXValue(key)
            flattenedDictList.add(keyGtxString)

            val content: GTXValue = dictGTXValue.get(key)!!
            flattenedDictList.add(content)

        }
        return buildSubTree(flattenedDictList)
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
                is DictGTXValue -> buildFromDictGTXValue(leaf)
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

