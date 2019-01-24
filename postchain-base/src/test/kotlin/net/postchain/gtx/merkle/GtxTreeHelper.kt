package net.postchain.gtx.merkle

import net.postchain.gtx.ArrayGTXValue
import net.postchain.gtx.DictGTXValue
import net.postchain.gtx.GTXValue
import net.postchain.gtx.IntegerGTXValue

object GtxTreeHelper {
    /**
     * Transforms (readable) integers into [ArrayGTXValue]
     */
    fun transformIntListToArrayGTXValue(ints: List<Int>): ArrayGTXValue {
        return transformGTXsToArrayGTXValue(transformIntToGTXValue(ints))
    }

    /**
     * Transforms (readable) integers into [IntegerGTXValue] list
     */
    fun transformIntToGTXValue(ints: List<Int>): MutableList<GTXValue> {
        val retList = arrayListOf<GTXValue>()
        for (i in ints) {
            retList.add(IntegerGTXValue(i.toLong()))
        }
        return retList
    }

    /**
     * Packs a list of [GTXValue] into an [ArrayGTXValue]
     */
    fun transformGTXsToArrayGTXValue(gtxList: List<GTXValue>): ArrayGTXValue {
        val gtxArr: Array<GTXValue> = gtxList.toTypedArray()
        return ArrayGTXValue(gtxArr)
    }

    /**
     * Transforms (readable) strings and integers into a [DictGTXValue]
     */
    fun transformStringAndIntToDictGTXValue(strings: List<String>, ints: List<Int>): DictGTXValue {
        if (strings.size != ints.size) {
            throw IllegalArgumentException("Cannot make a Dict if we don't have equal amount of keys and content")
        }
        val dict = HashMap<String, GTXValue>()

        for (i in 0..(strings.size - 1)) {
            val key = strings[i]
            val content = IntegerGTXValue(ints[i].toLong())
            dict.set(key, content)
        }
        return DictGTXValue(dict)

    }
}