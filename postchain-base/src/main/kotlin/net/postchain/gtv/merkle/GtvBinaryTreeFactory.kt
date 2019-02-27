package net.postchain.gtv.merkle

import net.postchain.base.merkle.*
import net.postchain.base.merkle.proof.MerkleProofTreeFactory

import net.postchain.gtv.*
import net.postchain.gtv.merkle.factory.GtvBinaryTreeFactoryArray
import net.postchain.gtv.merkle.factory.GtvBinaryTreeFactoryDict
import net.postchain.gtv.path.GtvPath
import net.postchain.gtv.path.GtvPathSet


/**
 * This can build two types of trees:
 * 1. Make a binary tree out of a Gtv object graph
 * 2. Same as above, but we also marked each Gtv sub structure that should be a path leaf.
 *    If you want this option (2) you have to provide a list of [GtvPath]
 */
class GtvBinaryTreeFactory() : BinaryTreeFactory<Gtv, GtvPathSet>() {

    /**
     * Generic builder.
     * @param Gtv will take any damn thing
     */
    fun buildFromGtv(gtv: Gtv, memoization: MerkleHashMemoization<Gtv>): GtvBinaryTree {
        return buildFromGtvAndPath(gtv, GtvPath.NO_PATHS, memoization)
    }

    /**
     * Generic builder.
     * @param Gtv will take any damn thing
     * @param GtvPathList will tell us what element that are path leafs
     */
    fun buildFromGtvAndPath(gtv: Gtv, gtvPaths: GtvPathSet, memoization: MerkleHashMemoization<Gtv>): GtvBinaryTree {
        if (MerkleProofTreeFactory.logger.isDebugEnabled) {
            MerkleProofTreeFactory.logger.debug("--------------------------------------------")
            MerkleProofTreeFactory.logger.debug("--- Converting GTV to binary tree ----------")
            MerkleProofTreeFactory.logger.debug("--------------------------------------------")
        }
        val result = handleLeaf(gtv, gtvPaths, memoization, true)
        if (MerkleProofTreeFactory.logger.isDebugEnabled) {
            MerkleProofTreeFactory.logger.debug("--------------------------------------------")
            MerkleProofTreeFactory.logger.debug("--- /Converting GTV to binary tree ---------")
            MerkleProofTreeFactory.logger.debug("--------------------------------------------")
        }
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
    fun buildLeafElements(leafList: List<Gtv>, gtvPaths: GtvPathSet, memoization: MerkleHashMemoization<Gtv>): ArrayList<BinaryTreeElement> {
        val leafArray = arrayListOf<BinaryTreeElement>()

        val onlyArrayPaths = gtvPaths.keepOnlyArrayPaths() // For performance, since we will loop soon

        for (i in 0..(leafList.size - 1)) {
            val pathsRelevantForThisLeaf = onlyArrayPaths.getTailIfFirstElementIsArrayOfThisIndexFromList(i)
            //println("New paths, (size: ${pathsRelevantForThisLeaf.size} ) list: " + GtvPath.debugRerpresentation(pathsRelevantForThisLeaf))
            val leaf = leafList[i]
            val binaryTreeElement = handleLeaf(leaf, pathsRelevantForThisLeaf, memoization)
            leafArray.add(binaryTreeElement)
        }
        return leafArray
    }

    override fun getEmptyPathSet(): GtvPathSet = GtvPath.NO_PATHS

    /**
     * At this point we should have looked in cache.
     *
     * @param leaf we should turn into a tree element
     * @param gtvPaths
     * @param memoization is not used for this leaf (since we know it's not in cache) but might be used below
     * @return the tree element we created.
     */
    override fun innerHandleLeaf(leaf: Gtv, gtvPaths: GtvPathSet, memoization: MerkleHashMemoization<Gtv>): BinaryTreeElement {
        //println("handleLeaf, Proof path (size: ${GtvPaths.size} ) list: " + GtvPath.debugRerpresentation(GtvPaths))
        return when (leaf) {
            is GtvPrimitive  -> handlePrimitiveLeaf(leaf, gtvPaths)
            is GtvArray      -> GtvBinaryTreeFactoryArray.buildFromGtvArray(leaf, gtvPaths, memoization)
            is GtvDictionary -> GtvBinaryTreeFactoryDict.buildFromGtvDictionary(leaf, gtvPaths, memoization)
            is GtvCollection -> throw IllegalStateException("Programmer should have dealt with this container type: ${leaf.type}")
            else ->             throw IllegalStateException("What is this? Not container and not primitive? type: ${leaf.type}")
        }
    }

    override fun getNrOfBytes(leaf: Gtv): Int {
        return leaf.nrOfBytes()
    }


}