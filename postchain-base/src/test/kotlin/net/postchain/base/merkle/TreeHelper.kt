package net.postchain.base.merkle

import net.postchain.gtx.*

object TreeHelper {

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

    fun transformIntToHash(hexStrings: List<String>): MutableList<Hash> {
        val retList = arrayListOf<Hash>()
        for (hexStr in hexStrings) {
            val b = hexStr.toByte()
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

    /**
     * @return a [ByteArray] from a hex string
     */
    fun convertToByteArray(hexString: String): ByteArray {
        val len = hexString.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hexString.get(i), 16) shl 4) +
                    Character.digit(hexString.get(i + 1), 16)).toByte()
            i += 2
        }
        return data
    }

    /**
     * Sometimes we need to strip white spaces and newlines etc for string comparison.
     */
    fun stripWhite(s: String): String {
        return s.replace("\\s".toRegex(), "")
    }

}