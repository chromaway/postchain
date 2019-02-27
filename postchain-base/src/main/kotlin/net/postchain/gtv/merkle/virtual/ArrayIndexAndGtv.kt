package net.postchain.gtv.merkle.virtual

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvVirtualArray
import kotlin.math.roundToInt

/**
 * @property index is the
 * @property value is the [Gtv] value we want to keep in the
 * @property depth is how many steps from the
 */
data class ArrayIndexAndGtv(var index: Int, val value: Gtv) {

    /*
    TODO:POS-8 Delete this if not used after 2019-06-01
    fun updateIndex() {
        height++
        val valueToAdd = Math.pow(2.0, height.toDouble()).roundToInt()
        index += valueToAdd
    }

    fun updateHeightOnly() {
        height++
    }
    */
}


data class ArrayIndexAndGtvSet(val innerSet: MutableSet<ArrayIndexAndGtv>) {

    /**
     * Sometimes we need an empty set
     */
    constructor(): this(mutableSetOf())

    /**
     * Usually we begin with a set of only one value
     */
    constructor(value: Gtv): this(mutableSetOf(ArrayIndexAndGtv(0, value)))

    fun addAll(otherSet: ArrayIndexAndGtvSet) {
        innerSet.addAll(otherSet.innerSet)
    }

    /**
     * Turns the elements into a virtual array (put "null" in all empty positions)
     */
    fun buildGtvVirtualArray(arrSize: Int): GtvVirtualArray {
        val retArr: Array<Gtv?> = Array(arrSize){null}
        for (element in innerSet) {
            retArr[element.index] = element.value
        }
        println("Virtual array: $retArr")
        return GtvVirtualArray(retArr)
    }

    /**
     * TODO:POS-8 Delete this if not used after 2019-06-01
     * Every time we move up in depth (from the right side) we need to increase the index value.
    fun updateAllIndexes() {
        for (element in innerSet) {
            element.updateIndex()
        }
    }

    fun updateAllHeights() {
        for (element in innerSet) {
            element.updateHeightOnly()
        }
    }
     */


}