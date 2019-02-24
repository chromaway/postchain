package net.postchain.api.rest.model

import net.postchain.common.hexStringToByteArray

class GTXQuery(private val hex: String) {

    val bytes: ByteArray
        get() {
            return hex.hexStringToByteArray()
        }

    override fun equals(other: Any?): Boolean {
        if (other !is GTXQuery) {
            return false
        }
        return other.bytes.contentEquals(bytes)
    }

    override fun hashCode(): Int {
        return hex.hashCode()
    }
}