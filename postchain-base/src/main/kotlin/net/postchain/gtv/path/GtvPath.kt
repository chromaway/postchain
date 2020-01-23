// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.path

import mu.KLogging
import net.postchain.base.path.Path

/**
 * [GtvPath] is used for referencing a sub-structure of a Gtv graph (a mix of arrays and dictionaries)
 */


/**
 * A [GtvPath] is a list of instructions how to navigate to the next element in the structure of arrays and dictionaries
 *
 * If the path is a "leaf" we are at the very bottom and should not go any deeper.
 *
 * @property pathElements is the list of all path elements, sorted in correct order.
 */
class GtvPath(pathElements: List<GtvPathElement>): Path<GtvPathElement>(pathElements) {

    /**
     * (Kotlin doesn't have tail!?)
     *
     * @return a new [GtvPath] with the tail of the path
     */
    fun tail(): GtvPath {
        if (pathElements.isEmpty()) {
            throw IllegalArgumentException("Impossible to tail this array")
        }
        val tail = pathElements.subList(1, pathElements.size)
        return GtvPath(tail)
    }

    // For debug
    fun debugString(): String {
        val sb = StringBuffer()
        for (elem in pathElements) {
            when (elem) {
                is SearchableGtvPathElement -> {
                    sb.append("-> " + elem.getSearchKey())
                }
                is GtvPathLeafElement -> {
                    sb.append("-> Leaf")
                }
            }
        }
        return sb.toString()
    }

    // All the relevalt [GtvPathElement] subclasses should have equals overridden.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        val myOther = other as GtvPath
        return pathElements == myOther.pathElements
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
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
        fun getTailIfFirstElementIsDictOfThisKey(dictKey: String, gtvPath: GtvPath): GtvPath? {
            return genericGetTail(dictKey, gtvPath)
        }

        /**
         * Internal impl
         */
        private fun <T>genericGetTail(searchKey: T, gtvPath: GtvPath): GtvPath? {
            if (searchKey == null) {
                throw IllegalArgumentException("Have to provide a search key")
            }

            val firstElement = try {
                 gtvPath.pathElements.first()
            } catch (e: NoSuchElementException) {
                logger.debug("Why are we dropping first element of an empty path?") // This should not happen, so maybe warning?
                return null
            }

            if (firstElement is SearchableGtvPathElement)  {
                if (firstElement.getSearchKey().toString() == searchKey.toString()) { // Don't know why Kotlin does this!! (shouldn't have to do toString())
                    // We have a match, then we can remove this element
                    return gtvPath.tail() as GtvPath
                }
            }
            return null
        }
    }

}

