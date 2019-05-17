package net.postchain.gtv.merkle.virtual

import net.postchain.base.merkle.proof.MerkleProofElement
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvVirtualArray

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


data class ArrayIndexAndGtvList(val innerSet: MutableList<ArrayIndexAndGtv>) {

    /**
     * Sometimes we need an empty list
     */
    constructor(): this(mutableListOf())

    /**
     * Usually we begin with a list of only one value
     */
    constructor(index: Int, value: Gtv): this(mutableListOf(ArrayIndexAndGtv(index, value)))

    fun addAll(otherSet: ArrayIndexAndGtvList) {
        innerSet.addAll(otherSet.innerSet)
    }

    /**
     * Turns the elements into a virtual array (put "null" in all empty positions)
     */
    fun buildGtvVirtualArray(proofElement: MerkleProofElement, arrSize: Int): GtvVirtualArray {
        val retArr: Array<Gtv?> = Array(arrSize){null}
        for (element in innerSet) {
            retArr[element.index] = element.value
        }
        return GtvVirtualArray(proofElement, retArr)
    }

}