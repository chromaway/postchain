package net.postchain.gtx

import java.util.LinkedList
import mu.KLogging
import net.postchain.base.merkle.MerklePathSet

/**
 * [GTXPath] is used for referencing a sub-structure of a GTX graph (a mix of arrays and dictionaries)
 */


sealed class GTXPathElement {

}

abstract class SearchableGTXPathElement: GTXPathElement() {

    /**
     * This class has a search key, that can be used to find the next element
     */
    abstract fun getSearchKey(): Any
}

/**
 * Represents an index position in a [ArrayGTXValue]
 */
data class ArrayGTXPathElement(val index: Int): SearchableGTXPathElement() {
    override fun getSearchKey(): Any = index
}

/**
 * Represents what key to use in a [DictGTXValue]
 */
data class DictGTXPathElement(val key: String): SearchableGTXPathElement() {
    override fun getSearchKey(): Any = key
}

/**
 * The last element of the path
 */
object LeafGTXPathElement: GTXPathElement() { }


/**
 * A [GTXPath] is a list of instructions how to navigate to the next element in the structure of arrays and dictionaries
 *
 * If the path is a "leaf" we are at the very bottom and should not go any deeper.
 */
class GTXPath(val pathElements: List<GTXPathElement>): KLogging()  {

    /**
     * (Kotlin doesn't have tail!?)
     *
     * @return a new [GTXPath] with the tail of the path
     */
    fun tail(): GTXPath {
        if (pathElements.isEmpty()) {
            throw IllegalArgumentException("Impossible to tail this args")
        }
        val tail: List<GTXPathElement> = pathElements.subList(1, pathElements.size)
        return GTXPath(tail)
    }

    /**
     * @return true if there is only a leaf left
     */
    fun isAtLeaf(): Boolean {
        val firstElement = pathElements.first()
        if (firstElement == null) {
            logger.warn("Why are using an empty path?") // This should not happen, so maybe warning?
            return true
        }
        return firstElement is LeafGTXPathElement
    }

    // For debug
    fun size(): Int {
        return this.pathElements.size
    }

    // For debug
    fun debugString(): String {
        val sb = StringBuffer()
        for (elem in pathElements) {
            when (elem) {
                is SearchableGTXPathElement -> {
                    sb.append("-> " + elem.getSearchKey())
                }
                is LeafGTXPathElement -> {
                    sb.append("-> Leaf")
                }
            }
        }
        return sb.toString()
    }


    companion object GTXPathList: KLogging()  {

        /**
         * Please don't add anything to this list, it is supposed to be empty :-D
         */
        val NO_PATHS: GTXPathSet = GTXPathSet(setOf())

        // Some debug output
        fun debugRerpresentation(paths: List<GTXPath>): String {
            val sb = StringBuffer()
            for (path in paths) {
                sb.append(path.debugString() + "\n")
            }
            return sb.toString()
        }


        /**
         * @return If the first element of [GTXPath] matches the given "next" [GTXValue],
         *  then return the tail (a new [GTXPath] with where the first object has been removed).
         *  Else return nothing.
         */
        fun getTailIfFirstElementIsArrayOfThisIndex(arrayIndex: Int, gtxPath: GTXPath): GTXPath? {
            return genericGetTail(arrayIndex, gtxPath)
        }


        /**
         * @return If the first element of [GTXPath] matches the given "next" [GTXValue],
         *  then return the tail (a new [GTXPath] with where the first object has been removed).
         *  Else return nothing.
         */
        fun getTailIfFirstElementIsDictOfThisKey(dictKey: String, gtxPath: GTXPath): GTXPath? {
            return genericGetTail(dictKey, gtxPath)
        }

        /**
         * Internal impl
         */
        private fun <T>genericGetTail(searchKey: T, gtxPath: GTXPath): GTXPath? {
            if (searchKey == null) {
                throw IllegalArgumentException("Have to provide a search key")
            }

            val firstElement = gtxPath.pathElements.first()
            if (firstElement == null) {
                logger.debug("Why are we dropping first element of an empty path?") // This should not happen, so maybe warning?
                return null
            }

            if (firstElement is SearchableGTXPathElement)  {
                if (firstElement.getSearchKey().toString() == searchKey.toString()) { // Don't know why Kotlin does this!! (shouldn't have to do toString())
                    // We have a match, then we can remove this element
                    return gtxPath.tail()
                }
            }
            return null
        }
    }




    // TODO:  We might not need this at all. If not used 2019-04-01 plz remove
    /**
     * @return The [GTXValue] that the path leads to, or null if the path cannot be followed
     */
    fun getLeafFromGTXGraph(root: GTXValue): GTXValue? {
        var currentGTXValue: GTXValue = root
        var counter= 0
        for (pathElement in pathElements) {
            when (pathElement) {
                is ArrayGTXPathElement -> {
                    if (currentGTXValue is ArrayGTXValue) {
                        if (pathElement.index <= currentGTXValue.getSize()) {
                            currentGTXValue = currentGTXValue.array[pathElement.index]
                        } else {
                            logger.debug("Incorrect path! Array index: ${pathElement.index} out of bounds, path at element: $counter")
                            return null
                        }
                    } else {
                        logger.debug("Incorrect path! Expected args at element: $counter")
                        return null
                    }
                }
                is DictGTXPathElement -> {
                    if (currentGTXValue is DictGTXValue) {
                        val found: GTXValue? = currentGTXValue.get(pathElement.key)
                        if (found != null) {
                            currentGTXValue = found
                        } else {
                            logger.debug("Incorrect path! Dict key: ${pathElement.key} not found, path at element: $counter")
                            return null
                        }
                    } else {
                        logger.debug("Incorrect path! Expected a dict at element: $counter")
                        return null
                    }
                }
            }
            counter++
        }
        return currentGTXValue
    }

}


object GTXPathFactory {

    /**
     * Use this constructor to convert a weakly typed path to a [GTXPath]
     *
     * @param inputArr is just an args with Ints and Strings representing the path
     * @return a [GTXPath] (same same by well typed)
     */
    fun buildFromArrayOfPointers(inputArr: Array<Any>): GTXPath {
        val pathElementList = LinkedList<GTXPathElement>()
        for (item in inputArr) {
            when (item) {
                is Int -> {
                    pathElementList.add(ArrayGTXPathElement(item))
                }
                is String -> {
                    pathElementList.add(DictGTXPathElement(item))
                }
                else -> throw IllegalArgumentException("A path structure must only consist of Ints and Strings, not $item")
            }
        }
        // Add one last element
        pathElementList.add(LeafGTXPathElement)
        return GTXPath(pathElementList)
    }
}


/**
 * A collection of [GTXPath]s. Order among the paths is not important
 */
class GTXPathSet(val paths: Set<GTXPath>): MerklePathSet {


    override fun isEmpty(): Boolean {
        return paths.isEmpty()
    }

    /**
     * @return true is any of the paths in the list is (just) a leaf
     */
    override fun isThisAProofLeaf(): Boolean {
        return paths.any{ it.isAtLeaf() }
    }

    // ----------- Filter on type of next path element ---------
    fun keepOnlyArrayPaths(): GTXPathSet {
        val filteredPaths = paths.filter { it.pathElements.first() is ArrayGTXPathElement }
        return GTXPathSet(filteredPaths.toSet())
    }

    fun keepOnlyDictPaths(): GTXPathSet {
        val filteredPaths = paths.filter { it.pathElements.first() is DictGTXPathElement }
        return GTXPathSet(filteredPaths.toSet())
    }

    // ----------- Filter on index/key ---------

    /**
     * @param arrayIndex the index we are looking for
     * @return A new path set, where all [GTXPath] without a match has been filtered out
     *         and the one that remain only hold the tail.
     */
    fun getTailIfFirstElementIsArrayOfThisIndexFromList(arrayIndex: Int): GTXPathSet {
        return genericGetTailFormList(arrayIndex, GTXPath.GTXPathList::getTailIfFirstElementIsArrayOfThisIndex)
    }

    /**
     * @param dictKey the key we are looking for
     * @return A new path set, where all [GTXPath] without a match has been filtered out
     *         and the one that remain only hold the tail.
     */
    fun getTailIfFirstElementIsDictOfThisKeyFromList(dictKey: String): GTXPathSet {
        return genericGetTailFormList(dictKey, GTXPath.GTXPathList::getTailIfFirstElementIsDictOfThisKey)
    }

    /**
     * Internal impl (will work with any search key type)
     */
    private fun <T>genericGetTailFormList(seachKey: T, filterFun: (T, GTXPath) -> GTXPath?): GTXPathSet {
        val retGtxPaths = arrayListOf<GTXPath>()
        for (gtxPath in paths) {
            val newPath = filterFun(seachKey, gtxPath)
            if (newPath != null) {
                retGtxPaths.add(newPath)
            }
        }
        return GTXPathSet(retGtxPaths.toSet())
    }
}