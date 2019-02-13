package net.postchain.base.merkle

import net.postchain.base.merkle.MerkleBasics.HASH_PREFIX_LEAF
import net.postchain.base.merkle.MerkleBasics.HASH_PREFIX_NODE


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
 *  Our rule for transforming an args into a [ContentLeafFullBinaryTree] is illustrated by Example3 below:
 *  -------------
 *  Example3:
 *
 *  [1 2 3 4 5 6 7]
 *  -------------
 *  As the name suggests, we have values/content in the leafs (but no content in the nodes):
 *  -------------
 *               args root
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
open class BinaryTreeElement() {

    // This will tell us if this structure is the leaf of a path
    // (A "path leaf" is something we will not convert to a hash in a merkle proof tree)
    private var isPathLeaf = false
    fun setPathLeaf(isLeaf: Boolean) { isPathLeaf = isLeaf }
    fun isPathLeaf() = isPathLeaf

    /**
     * Get the prefix belonging to this instance
     */
    open fun getPrefixByte(): Byte = HASH_PREFIX_NODE // Usually overidden

    /**
     * Get the number of bytes this element represents
     */
    open fun getNrOfBytes(): Int = throw IllegalStateException("Should implement this in sub class")
}

/**
 * Super type of binary (parent) nodes.
 * Doesn't hold any content.
 */
open class Node(val left: BinaryTreeElement, val right: BinaryTreeElement): BinaryTreeElement() {

    override fun getPrefixByte(): Byte = HASH_PREFIX_NODE
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
data class Leaf<T>(val content: T, val leafIsPathLeaf: Boolean = false, val sizeInBytes: Int): BinaryTreeElement() {
    init {
        setPathLeaf(leafIsPathLeaf)
    }

    override fun getPrefixByte(): Byte = HASH_PREFIX_LEAF

    override fun getNrOfBytes(): Int = sizeInBytes
}

/**
 * Dummy filler. Will always be the right side.
 * (This is needed for the case when an args only has one element)
 */
object EmptyLeaf: BinaryTreeElement() {
    override fun getNrOfBytes() = 0
}

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



