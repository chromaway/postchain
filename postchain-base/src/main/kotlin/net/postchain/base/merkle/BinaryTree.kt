// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.merkle

import net.postchain.base.merkle.MerkleBasics.HASH_PREFIX_LEAF
import net.postchain.base.merkle.MerkleBasics.HASH_PREFIX_NODE
import net.postchain.base.merkle.proof.MerkleHashSummary
import net.postchain.base.path.PathElement
import net.postchain.base.path.PathLeafElement
import net.postchain.core.UserMistake


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
open class BinaryTreeElement {

    /**
     * "pathElem" will tell us if this element is part of a path.
     *
     * Note: It would be more correct to store a set of [PathElement] , because there might be multiple paths leading
     *       down from this object. So far we have no need of keeping track of that, so we just pick one if there are many.
     */
    private var pathElem: PathElement? = null

    /**
     * @return a [PathElement] if this element is part of a path (and COULD be a leaf) or null.
     */
    fun getPathElement() = pathElem

    /**
     * @return true if the element is part of a path.
     */
    fun isPath() = pathElem != null

    /**
     * @return True if this element is a leaf of a path (i.e. it should be proven).
     */
    fun isPathLeaf(): Boolean {
        return if (pathElem != null) {
            pathElem is PathLeafElement
        } else {
            false
        }
    }

    /**
     * This method is protected for a reason: we want to be very strict about when and who sets the "pathElem".
     */
    protected fun setPathElement(pathElem: PathElement?) {
        this.pathElem = pathElem
    }

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
 *
 * Note: as you see a [Node] must have both a left and a right side.
 *
 * @property left is the sub tree of the left side
 * @property right is the sub tree of the right side.
 */
open class Node(val left: BinaryTreeElement, val right: BinaryTreeElement): BinaryTreeElement() {

    override fun getPrefixByte(): Byte = HASH_PREFIX_NODE
}

/**
 * Represents a node that is a root of it's own sub tree.
 * The [SubTreeRootNode] can be proven, and should also have a reference to the original structure (content)
 *
 * @property left is the left side of the sub tree
 * @property right is the right side of the sub tree
 * @property content is the original object that generated tho sub tree (it is valuable to hold on to it for later)
 * @property pathElem should be not tull if the sub tree/original structure is part of a path.
 */
open class SubTreeRootNode<T>(left: BinaryTreeElement, right: BinaryTreeElement, val content: T, pathElem: PathElement? = null): Node(left, right) {

    init {
        setPathElement(pathElem)
    }
}

/**
 *  Holds content of type (T).
 *
 *  @property content holds the original "leaf object".
 *  @property sizeInBytes is the number of bytes the original "leaf object" takes up.
 *  @property pathElem should be not null if the leaf is part of a path (If so it must be the END DESTINATION of the path)
 */
class Leaf<T>(val content: T, val sizeInBytes: Int, pathElem: PathElement? = null): BinaryTreeElement() {
    init {
        if (pathElem != null) {
            if (pathElem is PathLeafElement) {
                setPathElement(pathElem)
            } else {
                throw UserMistake("The path and object structure does not match! We are at a leaf, but the path expects a sub structure. Path element: $pathElem , content: $content")
            }
        }
    }

    override fun getPrefixByte(): Byte = HASH_PREFIX_LEAF

    override fun getNrOfBytes(): Int = sizeInBytes
}


/**
 *  Holds the merkle root of the structure (from the cache)
 *
 *  Note: we can ONLY use this type if there is no path leading in here.
 *
 *  @property cachedSummary is the summary we found in the cache for this leaf
 */
data class CachedLeaf(val cachedSummary: MerkleHashSummary): BinaryTreeElement() {
    init {
        setPathElement(null) // We cannot use
    }

    override fun getPrefixByte(): Byte {
        throw IllegalStateException("Why are we asking for a prefix on a hash we found in the cache?")
    }

    override fun getNrOfBytes(): Int = cachedSummary.nrOfBytes
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
 *
 * @property root is the root of the (whole) binary tree
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



