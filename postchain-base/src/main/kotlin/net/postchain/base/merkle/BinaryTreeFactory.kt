package net.postchain.base.merkle

import mu.KLogging


/**
 * The factory does the conversion between list of elements and tree of elements.
 *
 * Note: The idea is that you should sub class for each type of element (for example [GTXValue]) you want to build.
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