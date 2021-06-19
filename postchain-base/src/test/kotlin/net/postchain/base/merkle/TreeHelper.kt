// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.merkle

import net.postchain.common.data.Hash

object TreeHelper {

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
     * @return A readable HEX string of the ByteArray
     */
    fun convertToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
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