package net.postchain.gtx.merkle.factory

import net.postchain.base.merkle.BinaryTreeElement
import net.postchain.base.merkle.EmptyLeaf
import net.postchain.base.merkle.Node
import net.postchain.gtx.*
import net.postchain.gtx.merkle.GtxBinaryTreeFactory
import net.postchain.gtx.merkle.GtxDictHeadNode
import java.util.SortedSet

object GtxBinaryTreeFactoryDict {


    private val mainFactory = GtxBinaryTreeFactory()

    /**
     * The strategy for transforming [DictGTXValue] is pretty simple, we treat the key and value as leafs and
     * add them both as elements to the tree.
     *
     * There is an edge cases here:
     * - When the dict is empty. -> We return a top node with two empty leafs
     */
    fun buildFromDictGTXValue(dictGTXValue: DictGTXValue, gtxPaths: GTXPathSet): GtxDictHeadNode {
        val isThisAProofLeaf = gtxPaths.isThisAProofLeaf() // Will tell us if any of the paths points to this element
        //println("Dict,(is proof? $isThisAProofLeaf) Proof path (size: ${gtxPathList.size} ) list: " + GTXPath.debugRerpresentation(gtxPathList))
        val keys: SortedSet<String> = dictGTXValue.dict.keys.toSortedSet() // Needs to be sorted, or else the order is undefined

        if (keys.isEmpty()) {
            return GtxDictHeadNode(EmptyLeaf, EmptyLeaf, isThisAProofLeaf, dictGTXValue, keys.size)
        }

        // 1. Build first (leaf) layer
        val leafArray = buildLeafElementFromDict(keys, dictGTXValue, gtxPaths)

        // 2. Build all higher layers
        val result = mainFactory.buildHigherLayer(1, leafArray)

        // 3. Fix and return the root node
        val orgRoot = result.get(0)
        return when (orgRoot) {
            is Node -> GtxDictHeadNode(orgRoot.left, orgRoot.right, isThisAProofLeaf, dictGTXValue, keys.size)
            else -> throw IllegalStateException("Should not find element of this type here: $orgRoot")
        }
    }


    /**
     * Converts the key-value pairs of a [DictGTXValue] into [BinaryTreeElement] s.
     * The only tricky bit of this method is that we need to remove paths that are irrelevant for the pair in question.
     *
     * @param leafList the list of [GTXValue] we will use for leafs in the tree
     * @param gtxPaths the paths we have to consider while creating the leafs
     * @return an array of all the leafs as [BinaryTreeElement] s. Note that some leafs might not be primitive values
     *   but some sort of collection with their own leafs (recursivly)
     */
    private fun buildLeafElementFromDict(keys: SortedSet<String>, dictGTXValue: DictGTXValue, gtxPaths: GTXPathSet): ArrayList<BinaryTreeElement> {
        val leafArray = arrayListOf<BinaryTreeElement>()

        val onlyDictPaths = gtxPaths.keepOnlyDictPaths() // For performance, since we will loop soon

        for (key in keys) {

            //println("key extracted: $key")

            // 1.a Fix the key
            val keyGtxString: GTXValue = StringGTXValue(key)
            val keyElement = mainFactory.handlePrimitiveLeaf(keyGtxString, GTXPath.NO_PATHS) // The key cannot not be proved, so NO_PATHS
            leafArray.add(keyElement)

            // 1.b Fix the value/content
            val pathsRelevantForThisLeaf = onlyDictPaths.getTailIfFirstElementIsDictOfThisKeyFromList(key)
            val content: GTXValue = dictGTXValue.get(key)!!  // TODO: Is it ok to bang here if the dict is broken?
            val contentElement = mainFactory.handleLeaf(content, pathsRelevantForThisLeaf)
            leafArray.add(contentElement)
        }
        return leafArray
    }
}