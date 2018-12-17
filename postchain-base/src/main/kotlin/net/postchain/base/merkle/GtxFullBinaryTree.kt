package net.postchain.base.merkle

import net.postchain.gtx.*
import java.util.SortedSet


class GtxFullBinaryTree(root: FbtElement) : ContentLeafFullBinaryTree<GTXValue>(root) {


}

class GtxTreeElementFinder<T> {

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
            is Leaf<*> -> {
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


class GtxFullBinaryTreeFactory : CompleteBinaryTreeFactory<GTXValue>() {



    /**
     * Builds a [GtxFullBinaryTree]
     *
     * @param arrayGTXValue An [ArrayGTXValue] holding the components needed to build the tree
     */
    fun buildFromArray(arrayGTXValue: ArrayGTXValue): GtxFullBinaryTree {
        val result = buildFromArrayGTXValue(arrayGTXValue)
        return GtxFullBinaryTree(result)
    }


    fun buildFromArrayList(originalList: List<GTXValue>): GtxFullBinaryTree {
        val result = buildSubTree(originalList)
        return GtxFullBinaryTree(result)
    }

    /**
     * Builds a [GtxFullBinaryTree]
     *
     * @param dictGTXValue An [DictGTXValue] holding the components needed to build the tree
     */
    fun buildFromDict(dictGTXValue: DictGTXValue): GtxFullBinaryTree {
        val result = buildFromDictGTXValue(dictGTXValue)
        return GtxFullBinaryTree(result)
    }


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
     * Needed to override this, to handle [GTXValue] leafs
     */
    override fun handleLeaf(leaf: GTXValue): FbtElement {
        return when (leaf) {
            is ArrayGTXValue -> buildFromArrayGTXValue(leaf)
            is DictGTXValue -> buildFromDictGTXValue(leaf)
            else -> Leaf(leaf)
        }
    }
}