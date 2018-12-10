package net.postchain.base.merkle

import net.postchain.gtx.GTXValue
import net.postchain.gtx.IntegerGTXValue

object TreeHelper {

    /**
     * Useful for transforming readable
     */
    fun transformIntToGTXValue(intArray: ArrayList<Int>): ArrayList<GTXValue> {
        val retArr = arrayListOf<GTXValue>()
        for (i in intArray) {
            retArr.add(IntegerGTXValue(i.toLong()))
        }
        return retArr
    }

}