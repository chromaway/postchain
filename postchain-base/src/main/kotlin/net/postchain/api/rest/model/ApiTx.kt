package net.postchain.api.rest.model

import net.postchain.common.hexStringToByteArray

class ApiTx(val tx: String) {

    val bytes: ByteArray
        get() {
            return tx.hexStringToByteArray()
        }

    init {
        require(tx.length > 1) { "Tx length must be >= 2" }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ApiTx) {
            return false
        }
        return other.bytes.contentEquals(bytes)
    }

    override fun hashCode(): Int {
        return tx.hashCode()
    }
}