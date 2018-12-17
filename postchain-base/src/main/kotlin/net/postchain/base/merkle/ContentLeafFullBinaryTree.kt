package net.postchain.base.merkle

import mu.KLogging


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
data class Leaf<T>(val content: T): FbtElement()

/**
 * Wrapper class for the root object.
 * ("content leaf" is supposed to indicate that it's the Leaf that carries all the content of the tree)
 */
open class ContentLeafFullBinaryTree<T>(val root: FbtElement) {
    /**
     * Mostly for debugging
     */
    fun maxLevel(): Int {
        return maxLevelInternal(root)
    }

    private fun maxLevelInternal(node: FbtElement): Int {
        return when (node) {
            is Leaf<*> -> 1
            is Node -> maxOf(maxLevelInternal(node.left), maxLevelInternal(node.right)) + 1
        }
    }

}


/**
 * The factory does the actual conversion between list and tree.
 */
abstract class CompleteBinaryTreeFactory<T> : KLogging() {

    /**
     * Builds a [ContentLeafFullBinaryTree]
     *
     * @param originalList A collection of leafs used to create the tree
     */
    fun buildCompleteBinaryTree(originalList: List<T>): ContentLeafFullBinaryTree<T> {
        val result = buildSubTree(originalList)
        return ContentLeafFullBinaryTree(result)
    }


    /**
     * Builds the (sub?)tree from a list. We do this is in parts:
     *
     * 1. Transform each leaf in the list into a [Leaf]
     *    If a [GTXValue] proves to be a recursive type, make it into a [FbtElement]
     * 2. Create the nodes that exist above the leaf, all the way to the root.
     *
     * @return Root [FbtElement] node
     */
    protected fun buildSubTree(inList: List<T>): FbtElement {
        val leafArray = arrayListOf<FbtElement>()

        // 1. Build first (leaf) layer
        for (leaf: T in inList) {
            val locbtElement = handleLeaf(leaf)
            leafArray.add(locbtElement)
        }

        // 2. Build all higher layers
        val result = buildHigherLayer(1, leafArray)

        return result.get(0)
    }

    /**
     * Transforms the incoming leaf into an [FbtElement]
     */
    abstract fun handleLeaf(leaf: T): FbtElement


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

