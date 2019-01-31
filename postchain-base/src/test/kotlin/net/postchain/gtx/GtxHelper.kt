package net.postchain.gtx

object GtxHelper {

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
}