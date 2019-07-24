package net.postchain.base.path


/**
 * A collection of proof paths.
 */
interface PathSet {

    /**
     * @return true if there are no paths left
     */
    fun isEmpty(): Boolean

    /**
     * In the general case we have many paths, so we try to find any path that "is on" a leaf.
     * If there is no leaf, we will pick the current element from any path.
     *
     * @return a leaf path element if found, else a current path element, else null.
     */
    fun getPathLeafOrElseAnyCurrentPathElement(): PathElement?

}

