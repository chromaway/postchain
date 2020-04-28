// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.model

import java.util.*

class TxRID(val bytes: ByteArray) {
    init {
        require(bytes.size == 32) { "Hash must be exactly 32 bytes" }
    }

    override fun equals(other: Any?): Boolean {
        if (super.equals(other)) return true
        if (other !is TxRID) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(bytes)
    }
}