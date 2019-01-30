package net.postchain.gtv.merkle

import net.postchain.base.merkle.*

import net.postchain.gtv.*
import net.postchain.gtv.merkle.factory.GtvBinaryTreeFactoryArray
import net.postchain.gtv.merkle.factory.GtvBinaryTreeFactoryDict


/**
 * This can build two types of trees:
 * 1. Make a binary tree out of a Gtv object graph
 * 2. Same as above, but we also marked each Gtv sub structure that should be a path leaf.
 *    If you want this option (2) you have to provide a list of [GtvPath]
 */
class GtvBinaryTreeFactory : BinaryTreeFactory<Gtv, GtvPathSet>() {

    /**
     * Generic builder.
     * @param Gtv will take any damn thing
     */
    fun buildFromGtv(Gtv: Gtv): GtvBinaryTree {
        return buildFromGtvAndPath(Gtv, GtvPath.NO_PATHS)
    }

    /**
     * Generic builder.
     * @param Gtv will take any damn thing
     * @param GtvPathList will tell us what element that are path leafs
     */
    fun buildFromGtvAndPath(Gtv: Gtv, GtvPaths: GtvPathSet): GtvBinaryTree {
        val result = handleLeaf(Gtv, GtvPaths)
        return GtvBinaryTree(result)
    }


    /**
     * The generic method that builds [BinaryTreeElement] from [Gtv] s.
     * The only tricky bit of this method is that we need to remove paths that are irrelevant for the leaf in question.
     *
     * @param leafList the list of [Gtv] we will use for leafs in the tree
     * @param GtvPaths the paths we have to consider while creating the leafs
     * @return an array of all the leafs as [BinaryTreeElement] s. Note that some leafs might not be primitive values
     *   but some sort of collection with their own leafs (recursivly)
     */
    fun buildLeafElements(leafList: List<Gtv>, GtvPaths: GtvPathSet): ArrayList<BinaryTreeElement> {
        val leafArray = arrayListOf<BinaryTreeElement>()

        val onlyArrayPaths = GtvPaths.keepOnlyArrayPaths() // For performance, since we will loop soon

        for (i in 0..(leafList.size - 1)) {
            val pathsRelevantForThisLeaf = onlyArrayPaths.getTailIfFirstElementIsArrayOfThisIndexFromList(i)
            //println("New paths, (size: ${pathsRelevantForThisLeaf.size} ) list: " + GtvPath.debugRerpresentation(pathsRelevantForThisLeaf))
            val leaf = leafList[i]
            val binaryTreeElement = handleLeaf(leaf, pathsRelevantForThisLeaf)
            leafArray.add(binaryTreeElement)
        }
        return leafArray
    }

    /**
     * Handles different types of [Gtv] values
     */
    override fun handleLeaf(leaf: Gtv, GtvPathSet: GtvPathSet?): BinaryTreeElement {
        val GtvPaths = if (GtvPathSet == null) {
            GtvPath.NO_PATHS
        } else {
            GtvPathSet
        }

        //println("handleLeaf, Proof path (size: ${GtvPaths.size} ) list: " + GtvPath.debugRerpresentation(GtvPaths))
        return when (leaf) {
            is GtvPrimitive  -> handlePrimitiveLeaf(leaf, GtvPaths)
            is GtvArray      -> GtvBinaryTreeFactoryArray.buildFromGtvArray(leaf, GtvPaths)
            is GtvDictionary -> GtvBinaryTreeFactoryDict.buildFromGtvDictionary(leaf, GtvPaths)
            is GtvCollection -> throw IllegalStateException("Programmer should have dealt with this container type: ${leaf.type}")
            else ->             throw IllegalStateException("What is this? Not container and not primitive? type: ${leaf.type}")
        }
    }
}