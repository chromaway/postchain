package net.postchain.gtv

import java.util.LinkedList
import mu.KLogging
import net.postchain.base.merkle.MerklePathSet

/**
 * [GtvPath] is used for referencing a sub-structure of a Gtv graph (a mix of arrays and dictionaries)
 */


sealed class GtvPathElement


abstract class SearchableGtvPathElement: GtvPathElement() {

    /**
     * This class has a search key, that can be used to find the next element
     */
    abstract fun getSearchKey(): Any
}

/**
 * Represents an index position in a [GtvArray]
 */
data class ArrayGtvPathElement(val index: Int): SearchableGtvPathElement() {
    override fun getSearchKey(): Any = index
}

/**
 * Represents what key to use in a [GtvDictionary]
 */
data class DictGtvPathElement(val key: String): SearchableGtvPathElement() {
    override fun getSearchKey(): Any = key
}

/**
 * The last element of the path
 */
object LeafGtvPathElement: GtvPathElement()


/**
 * A [GtvPath] is a list of instructions how to navigate to the next element in the structure of arrays and dictionaries
 *
 * If the path is a "leaf" we are at the very bottom and should not go any deeper.
 */
class GtvPath(val pathElements: List<GtvPathElement>): KLogging()  {

    /**
     * (Kotlin doesn't have tail!?)
     *
     * @return a new [GtvPath] with the tail of the path
     */
    fun tail(): GtvPath {
        if (pathElements.isEmpty()) {
            throw IllegalArgumentException("Impossible to tail this array")
        }
        val tail: List<GtvPathElement> = pathElements.subList(1, pathElements.size)
        return GtvPath(tail)
    }

    /**
     * @return true if there is only a leaf left
     */
    fun isAtLeaf(): Boolean {
        return try {
            val firstElement = pathElements.first()
            firstElement is LeafGtvPathElement
        } catch (e: NoSuchElementException) {
            // Will be thrown if there is no "first" element
            logger.warn("Why are using an empty path?") // This should not happen, so maybe warning?
            return true
        }
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
                is SearchableGtvPathElement -> {
                    sb.append("-> " + elem.getSearchKey())
                }
                is LeafGtvPathElement -> {
                    sb.append("-> Leaf")
                }
            }
        }
        return sb.toString()
    }


    companion object GtvPathList: KLogging()  {

        /**
         * Please don't add anything to this list, it is supposed to be empty :-D
         */
        val NO_PATHS: GtvPathSet = GtvPathSet(setOf())

        // Some debug output
        fun debugRerpresentation(paths: List<GtvPath>): String {
            val sb = StringBuffer()
            for (path in paths) {
                sb.append(path.debugString() + "\n")
            }
            return sb.toString()
        }


        /**
         * @return If the first element of [GtvPath] matches the given "next" [Gtv],
         *  then return the tail (a new [GtvPath] with where the first object has been removed).
         *  Else return nothing.
         */
        fun getTailIfFirstElementIsArrayOfThisIndex(arrayIndex: Int, gtxPath: GtvPath): GtvPath? {
            return genericGetTail(arrayIndex, gtxPath)
        }


        /**
         * @return If the first element of [GtvPath] matches the given "next" [Gtv],
         *  then return the tail (a new [GtvPath] with where the first object has been removed).
         *  Else return nothing.
         */
        fun getTailIfFirstElementIsDictOfThisKey(dictKey: String, gtxPath: GtvPath): GtvPath? {
            return genericGetTail(dictKey, gtxPath)
        }

        /**
         * Internal impl
         */
        private fun <T>genericGetTail(searchKey: T, gtxPath: GtvPath): GtvPath? {
            if (searchKey == null) {
                throw IllegalArgumentException("Have to provide a search key")
            }

            val firstElement = try {
                 gtxPath.pathElements.first()
            } catch (e: NoSuchElementException) {
                logger.debug("Why are we dropping first element of an empty path?") // This should not happen, so maybe warning?
                return null
            }

            if (firstElement is SearchableGtvPathElement)  {
                if (firstElement.getSearchKey().toString() == searchKey.toString()) { // Don't know why Kotlin does this!! (shouldn't have to do toString())
                    // We have a match, then we can remove this element
                    return gtxPath.tail()
                }
            }
            return null
        }
    }

}


object GtvPathFactory {

    /**
     * Use this constructor to convert a weakly typed path to a [GtvPath]
     *
     * @param inputArr is just an array with Ints and Strings representing the path
     * @return a [GtvPath] (same same by well typed)
     */
    fun buildFromArrayOfPointers(inputArr: Array<Any>): GtvPath {
        val pathElementList = LinkedList<GtvPathElement>()
        for (item in inputArr) {
            when (item) {
                is Int -> {
                    pathElementList.add(ArrayGtvPathElement(item))
                }
                is String -> {
                    pathElementList.add(DictGtvPathElement(item))
                }
                else -> throw IllegalArgumentException("A path structure must only consist of Ints and Strings, not $item")
            }
        }
        // Add one last element
        pathElementList.add(LeafGtvPathElement)
        return GtvPath(pathElementList)
    }
}


/**
 * A collection of [GtvPath]s. Order among the paths is not important
 */
class GtvPathSet(val paths: Set<GtvPath>): MerklePathSet {


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
    fun keepOnlyArrayPaths(): GtvPathSet {
        val filteredPaths = paths.filter { it.pathElements.first() is ArrayGtvPathElement }
        return GtvPathSet(filteredPaths.toSet())
    }

    fun keepOnlyDictPaths(): GtvPathSet {
        val filteredPaths = paths.filter { it.pathElements.first() is DictGtvPathElement }
        return GtvPathSet(filteredPaths.toSet())
    }

    // ----------- Filter on index/key ---------

    /**
     * @param arrayIndex the index we are looking for
     * @return A new path set, where all [GtvPath] without a match has been filtered out
     *         and the one that remain only hold the tail.
     */
    fun getTailIfFirstElementIsArrayOfThisIndexFromList(arrayIndex: Int): GtvPathSet {
        return genericGetTailFormList(arrayIndex, GtvPath.GtvPathList::getTailIfFirstElementIsArrayOfThisIndex)
    }

    /**
     * @param dictKey the key we are looking for
     * @return A new path set, where all [GtvPath] without a match has been filtered out
     *         and the one that remain only hold the tail.
     */
    fun getTailIfFirstElementIsDictOfThisKeyFromList(dictKey: String): GtvPathSet {
        return genericGetTailFormList(dictKey, GtvPath.GtvPathList::getTailIfFirstElementIsDictOfThisKey)
    }

    /**
     * Internal impl (will work with any search key type)
     */
    private fun <T>genericGetTailFormList(seachKey: T, filterFun: (T, GtvPath) -> GtvPath?): GtvPathSet {
        val retGtxPaths = arrayListOf<GtvPath>()
        for (gtxPath in paths) {
            val newPath = filterFun(seachKey, gtxPath)
            if (newPath != null) {
                retGtxPaths.add(newPath)
            }
        }
        return GtvPathSet(retGtxPaths.toSet())
    }
}