package net.postchain.gtx

/**
 * [GTXPath] is used for referencing a sub-structure of a GTX graph (a mix of arrays and dictionaries)
 */


sealed class GTXPathElement

/**
 * Represents an index position in a [ArrayGTXValue]
 */
data class ArrayGTXPathElement(val index: Int): GTXPathElement() { }

/**
 * Represents what key to use in a [DictGTXValue]
 */
data class DictGTXPathElement(val key: String): GTXPathElement() { }

/**
 * A [GTXPath] is a list of pointers to where to enter the next element in the structure
 */
class GTXPath(val pathElements: List<GTXPathElement>) {

    /**
     * @return The [GTXValue] that the path leads to, or null if the path cannot be followed
     */
    fun getLeafFromGTXGraph(root: GTXValue): GTXValue? {
        var currentGTXValue: GTXValue = root
        for (pathElement in pathElements) {
            when (pathElement) {
                is ArrayGTXPathElement -> {
                    if (currentGTXValue is ArrayGTXValue) {
                        if (pathElement.index <= currentGTXValue.getSize()) {
                            currentGTXValue = currentGTXValue.array[pathElement.index]
                        } else {
                            return null // The path is incorrect
                        }
                    } else {
                        return null // The path is incorrect
                    }
                }
                is DictGTXPathElement -> {
                    if (currentGTXValue is DictGTXValue) {
                        val found: GTXValue? = currentGTXValue.get(pathElement.key)
                        if (found != null) {
                            currentGTXValue = found
                        } else {
                            return null // The path is incorrect
                        }
                    } else {
                        return null // The path is incorrect
                    }
                }
            }
        }
        return currentGTXValue
    }

}


object GTXPathFactory {

    /**
     * Use this constructor to convert a weakly typed path to a [GTXPath]
     *
     * @param inputArr is just an array with Ints and Strings representing the path
     * @return a [GTXPath] (same same by well typed)
     */
    fun buildFromArrayOfPointers(inputArr: Array<Any>): GTXPath {
        val pathElementList = arrayListOf<GTXPathElement>()
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
        return GTXPath(pathElementList)
    }
}