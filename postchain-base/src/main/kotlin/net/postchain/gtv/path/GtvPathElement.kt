package net.postchain.gtv.path

import net.postchain.base.path.PathElement
import net.postchain.base.path.PathLeafElement
import net.postchain.gtv.*


/**
 * @property previous is needed because we need to find out how we got here
 */
sealed class GtvPathElement(val previous: SearchableGtvPathElement?): PathElement

/**
 * This element will hold a index/key to the next element in the path.
 *
 * Typically a path looks like this:
 *
 *   Searchable... ->  Searchable... -> Searchable... -> Leaf...
 */
abstract class SearchableGtvPathElement(previous: SearchableGtvPathElement?): GtvPathElement(previous) {

    /**
     * This class has a search key, that can be used to find the next element
     */
    abstract fun getSearchKey(): Any

    /**
     * Converts the path element to a [Gtv] instance.
     */
    abstract fun buildGtv(): Gtv

}

/**
 * Represents an index position in a [GtvArray]
 *
 * @property previous (see super)
 * @property index is the array index this path element points to
 */
class ArrayGtvPathElement(previous: SearchableGtvPathElement?, val index: Int): SearchableGtvPathElement(previous) {

    override fun getSearchKey(): Any = index

    override fun buildGtv(): Gtv = GtvInteger(index.toLong())

    // We deliberately do not care about the "previous" in this implementation
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayGtvPathElement

        if (index != other.index) return false

        return true
    }

    // We deliberately do not care about the "previous" in this implementation
    override fun hashCode(): Int {
        return index
    }

    // Only 4 debugging
    override fun toString(): String {
        return "ArrayGtvPathElement(index=$index)"
    }


}

/**
 * Represents what key to use in a [GtvDictionary]
 *
 * @property previous (see super)
 * @property key is the dictionary key this path element points to
 */
class DictGtvPathElement(previous: SearchableGtvPathElement?, val key: String): SearchableGtvPathElement(previous) {
    override fun getSearchKey(): Any = key

    override fun buildGtv(): Gtv = GtvString(key)

    // We deliberately do not care about the "previous" in this implementation
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DictGtvPathElement

        if (key != other.key) return false

        return true
    }

    // We deliberately do not care about the "previous" in this implementation
    override fun hashCode(): Int {
        return key.hashCode()
    }

    // Only 4 debugging
    override fun toString(): String {
        return "DictGtvPathElement(key='$key')"
    }


}

/**
 * The last element of the path
 *
 * @property previous here we don't allow a null (since a path with just a dummy leaf is meaningless).
 */
class GtvPathLeafElement(previous: SearchableGtvPathElement): GtvPathElement(previous), PathLeafElement {

    // All classes of the same type are considered the same (this is a dummy element anyway)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    // Only 4 debugging
    override fun toString(): String {
        return "GtvPathLeafElement()"
    }


}
