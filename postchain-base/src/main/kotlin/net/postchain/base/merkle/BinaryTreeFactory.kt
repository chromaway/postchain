package net.postchain.base.merkle

import mu.KLogging
import net.postchain.base.path.PathLeafElement
import net.postchain.base.path.PathSet
import net.postchain.core.UserMistake
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvPrimitive


/**
 * The factory does the conversion between list of elements and tree of elements.
 *
 * Note: The idea is that you should sub class for each type of element (for example [Gtv]) you want to build.
 */
abstract class BinaryTreeFactory<T,TPathSet: PathSet>() : KLogging() {


    /**
     * Transforms the incoming leaf into an [BinaryTreeElement]
     * The idea with this function is that it can be recursive (if the leaf in turn is complex object with sub objects).
     *
     * Note: If we don't have a path here we can try to find the leaf in the cache.
     *
     * @param leaf the raw data we should wrap in a leaf
     * @param paths a collection of proof paths that might point to this leaf
     * @param memoization is the cache we can use to find pre-calculated values
     * @param isRoot tells us if this is the top element (we should not search for top element in cache)
     * @return the resulting [BinaryTreeElement] the leaf got converted to
     */
    fun handleLeaf(leaf: T, paths: TPathSet, isRoot: Boolean = false): BinaryTreeElement {
        return if (paths.isEmpty() && !isRoot && (leaf is GtvPrimitive)) {
            innerHandleLeaf(leaf, getEmptyPathSet())
        } else {
            innerHandleLeaf(leaf, paths)
        }
    }

    protected abstract fun getEmptyPathSet(): TPathSet

    /**
     * At this point we should have looked in cache.
     *
     * @param leaf we should turn into a tree element
     * @param gtvPaths
     * @param memoization is not used for this leaf (since we know it's not in cache) but might be used below
     * @return the tree element we created.
     */
    protected abstract fun innerHandleLeaf(leaf: T, paths: TPathSet): BinaryTreeElement


    /**
     * Just like [handleLeaf] but we know that this leaf should not be a complex type, but something we can
     * immediately wrap
     */
    fun handlePrimitiveLeaf(leaf: T, paths: TPathSet): BinaryTreeElement {
        val pathElem = paths.getPathLeafOrElseAnyCurrentPathElement()
        if (pathElem != null && pathElem !is PathLeafElement) {
            throw UserMistake("Path does not match the tree structure. We are at a leaf $leaf but found path element $pathElem")
        }
        return Leaf(leaf, getNrOfBytes(leaf), pathElem)
    }

    /**
     * @param leaf the leaf we want to know the size of
     * @return Number of bytes this leaf consumes
     */
    abstract fun getNrOfBytes(leaf: T): Int

    /**
     * Calls itself until the return value only holds 1 element
     *
     * Note: This method can only create standard [Node] that fills up the area between the "top" and the leaves.
     *       These "in-between" nodes cannot be "path leaf" or have any interesting properties.
     *
     * @param layer What layer we aim calculate
     * @param inList The args of nodes we should build from
     * @return All [BinaryTreeElement] nodes of the next layer
     */
    fun buildHigherLayer(layer: Int, inList: List<BinaryTreeElement>): List<BinaryTreeElement> {

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
            logger.warn("Why didn't we build exactly the correct amount? Layer: $layer , residue: $nrOfNodesToCreate , input args size: ${inList.size}")
        }

        return buildHigherLayer((layer + 1), returnArray)

    }



}