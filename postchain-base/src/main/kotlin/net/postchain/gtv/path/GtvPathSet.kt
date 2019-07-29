package net.postchain.gtv.path

import net.postchain.base.path.PathSet
import net.postchain.base.path.PathElement
import net.postchain.base.path.PathLeafElement
import net.postchain.core.UserMistake


/**
 * A collection of [GtvPath]s. Order among the paths is not important
 *
 * @property paths is a set of all paths relevant for a specific proof
 */
class GtvPathSet(val paths: Set<GtvPath>): PathSet {


    override fun isEmpty(): Boolean {
        return paths.isEmpty()
    }

    override fun getPathLeafOrElseAnyCurrentPathElement(): PathElement? {
        var leafElem: GtvPathLeafElement? = null
        var currElem: GtvPathElement? = null
        var prev: Pair<GtvPath?, GtvPathElement?> = Pair(null, null)

        for (path in paths) {
            currElem = path.getCurrentPathElement()
            if (currElem is GtvPathLeafElement) {
                leafElem = currElem
            }

            prev = errorCheckUnequalParent(path, currElem, prev.first, prev.second)
        }

        return if (leafElem != null) {
            leafElem
        } else {
            currElem // It doesn't matter which one we return (Next step we will get the "previous" from this one)
        }
    }

    /**
     * Yeah, this might be a completely un-needed check (but it MIGHT save us later on if we forget this rule).
     * What we are looking for here is an impossible state where two paths in the same set don't have the same parent.
     * (Since we usually only have one path in a path set, this check should be cheap)
     */
    private fun errorCheckUnequalParent(
            currPath: GtvPath,
            currElem: GtvPathElement,
            prevPath: GtvPath?,
            prevElem: GtvPathElement?
    ): Pair<GtvPath, GtvPathElement> {
        if (prevElem != null) {
            if (currElem.previous != prevElem.previous) {
                throw IllegalStateException("Something is wrong, these paths do not have the same parent. ($currPath) ($prevPath")
            }
        }
        return Pair(currPath, currElem)
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