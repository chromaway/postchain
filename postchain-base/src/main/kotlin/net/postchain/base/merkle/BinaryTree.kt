package net.postchain.base.merkle

import mu.KLogging


/**
 * This is usually a "full" binary tree that stores values in the leafs only. The exception to the "empty nodes" is
 * when we need to store collection structures in nodes, so that they can be used for proofs.
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
 *               array root
 *             /          \
 *        node1234        node567
 *       /     \           /     \
 *   node12  node34      node56   7
 *   /  \     /  \       /   \
 *  1   2    3   4      5     6
 *  -------------
 *
 *  NOTE:
 *  These trees will typically not be "balanced" nor "complete" since we will transform arrays of arrays into trees,
 *  see tests for more info.
 *
 */

/**
 * The root class of the tree elements.
 *
 * All elements have some idea about if they are a (proof) path leaf or not.
 * If you are not building a proof, this will be false on all elements.
 * (Prefixes are used when a [Hash] is calculated for the node.)
 */
open class BinaryTreeElement {

    // This will tell us if this structure is the leaf of a path
    // (A "path leaf" is something we will not convert to a hash in a merkle proof tree)
    private var isPathLeaf = false
    fun setPathLeaf(isLeaf: Boolean) { isPathLeaf = isLeaf }
    fun isPathLeaf() = isPathLeaf

    /**
     * Get the prefix belonging to this instance
     */
    open fun getPrefixByte(): Byte = 0 // Usually overidden
}

/**
 * Super type of binary (parent) nodes.
 * Doesn't hold any content.
 */
open class Node(val left: BinaryTreeElement, val right: BinaryTreeElement): BinaryTreeElement() {

    companion object NodeCompanion{
        const val internalNodePrefixByte: Byte = 0
    }

    override fun getPrefixByte(): Byte = internalNodePrefixByte
}

/**
 * Represents a node that is a root of it's own sub tree.
 * The [SubTreeRootNode] can be proven, and should also have a reference to the original structure (content)
 */
open class SubTreeRootNode<T>(left: BinaryTreeElement, right: BinaryTreeElement, isProofLeaf: Boolean, val content: T): Node(left, right) {

    init {
        setPathLeaf(isProofLeaf)
    }
}

/**
 *  Holds content of type (T).
 *
 *  Can be set to to be a "path leaf"
 */
data class Leaf<T>(val content: T, val leafIsPathLeaf: Boolean = false): BinaryTreeElement() {
    init {
        setPathLeaf(leafIsPathLeaf)
    }

    companion object LeafCompanion{
        const val leafPrefixByte: Byte = 1
    }

    override fun getPrefixByte(): Byte = leafPrefixByte
}

/**
 * Dummy filler. Will always be the right side.
 * (This is needed for the case when an array only has one element)
 */
object EmptyLeaf: BinaryTreeElement()

/**
 * Wrapper class for the root object.
 * ("content leaf" is supposed to indicate that it's the Leaf that carries all the content of the tree)
 */
open class BinaryTree<T>(val root: BinaryTreeElement) {
    /**
     * Mostly for debugging
     */
    fun maxLevel(): Int {
        return maxLevelInternal(root)
    }

    private fun maxLevelInternal(node: BinaryTreeElement): Int {
        return when (node) {
            is EmptyLeaf -> 0 // Doesn't count
            is Leaf<*> -> 1
            is Node -> maxOf(maxLevelInternal(node.left), maxLevelInternal(node.right)) + 1
            else -> throw IllegalStateException("What is this type? $node")
        }
    }

}


/**
 * The factory does the actual conversion between list and tree.
 */
abstract class BinaryTreeFactory<T,TPath> : KLogging() {

    /**
     * Builds the (sub?)tree from a list. We do this is in parts:
     *
     * 1. Transform each leaf in the list into a [Leaf]
     * 2. Create the nodes that exist above the leaf, all the way to the root.
     *
     * Note: this implementation cannot do proof trees, have to be overridden for that.
     *
     * @param leafList the list of leafs we should build a sub tree from
     * @return Root [BinaryTreeElement] node of the generated sub tree
     */
    protected fun buildSubTreeFromLeafList(leafList: List<T>): BinaryTreeElement {

        val emptyPaths = listOf<TPath>()
        val leafArray = arrayListOf<BinaryTreeElement>()

        // 1. Build first (leaf) layer
        for (i in 0..(leafList.size - 1)) {
            //val pathsRelevantForThisLeaf = keepOnlyRelevantPathsFun(i, pathList)
            val leaf = leafList[i]
            val btElement = handleLeaf(leaf, emptyPaths)
            leafArray.add(btElement)
        }

        // 2. Build all higher layers
        val result = buildHigherLayer(1, leafArray)

        return result.get(0)
    }

    /**
     * Transforms the incoming leaf into an [BinaryTreeElement]
     */
    abstract fun handleLeaf(leaf: T, pathList: List<TPath>): BinaryTreeElement


    /**
     * Calls itself until the return value only holds 1 element
     *
     * Note: This method can only create standard [Node] that fills up the area between the "top" and the leaves.
     *       These "in-between" nodes cannot be "path leaf" or have any interesting properties.
     *
     * @param layer What layer we aim calculate
     * @param inList The array of nodes we should build from
     * @return All [BinaryTreeElement] nodes of the next layer
     */
    protected fun buildHigherLayer(layer: Int, inList: List<BinaryTreeElement>): List<BinaryTreeElement> {

        if (inList.isEmpty()) {
            throw IllegalStateException("Cannot work on empty arrays. Layer: $layer")
        } else if (inList.size == 1) {
            return inList
        }

        val returnArray = arrayListOf<BinaryTreeElement>()
        var nrOfNodesToCreate = inList.size / 2
        var leftValue: BinaryTreeElement? = null
        var isLeft = true
        for (element: BinaryTreeElement in inList) {
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

