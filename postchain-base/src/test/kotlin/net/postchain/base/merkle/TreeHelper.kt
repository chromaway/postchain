package net.postchain.base.merkle

import net.postchain.gtx.*

object TreeHelper {

    /**
     * Transforms (readable) integers into [IntegerGTXValue]
     */
    fun transformIntToGTXValue(ints: List<Int>): MutableList<GTXValue> {
        val retList = arrayListOf<GTXValue>()
        for (i in ints) {
            retList.add(IntegerGTXValue(i.toLong()))
        }
        return retList
    }

    fun transformIntToHash(ints: List<Int>): MutableList<Hash> {
        val retList = arrayListOf<Hash>()
        for (i in ints) {
            val b = i.toByte()
            val byteArr: Hash = byteArrayOf(b)
            retList.add(byteArr)
        }
        return retList
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

    /**
     * @return A readable HEX string of the ByteArray
     */
    fun convertToHex(bytes: ByteArray): String {
        val sb: StringBuilder = StringBuilder()
        for (b in bytes) {
            val st = String.format("%02X", b)
            sb.append(st)
        }
        return sb.toString()
    }


}