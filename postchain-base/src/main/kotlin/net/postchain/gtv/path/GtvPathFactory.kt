package net.postchain.gtv.path

import java.util.*


object GtvPathFactory {


    /**
     * Use this constructor to convert a weakly typed path to a [GtvPath]
     *
     * @param inputArr is just an array with Ints and Strings representing the path
     * @return a [GtvPath] (same same by well typed)
     */
    fun buildFromArrayOfPointers(inputArr: Array<Any>): GtvPath {
        val pathElementList = LinkedList<GtvPathElement>()
        var lastPathElem: SearchableGtvPathElement? = null
        for (item in inputArr) {
            val newElem = when (item) {
                is Int -> {
                    ArrayGtvPathElement(lastPathElem, item)
                }
                is String -> {
                    DictGtvPathElement(lastPathElem, item)
                }
                else -> throw IllegalArgumentException("A path structure must only consist of Ints and Strings, not $item")
            }
            pathElementList.add(newElem)
            lastPathElem = newElem
        }
        // Add one last element
        val lastOne =lastPathElem!!  // The last one must have a previous element, or else the proof is trivial.
        pathElementList.add(GtvPathLeafElement(lastOne))
        return GtvPath(pathElementList)
    }
}
