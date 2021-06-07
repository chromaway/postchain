// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.testinfra

import net.postchain.core.Transaction
import net.postchain.core.TxEContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

open class TestTransaction(
        val id: Int,
        val good: Boolean = true,
        val correct: Boolean = true
) : Transaction {

    override fun isCorrect(): Boolean {
        return correct
    }

    override fun isSpecial(): Boolean {
        return false
    }

    override fun apply(ctx: TxEContext): Boolean {
        return good
    }

    override fun getRawData(): ByteArray {
        return bytes(40)
    }

    override fun getRID(): ByteArray {
        return bytes(32)
    }

    override fun getHash(): ByteArray {
        return getRID().reversed().toByteArray()
    }

    private fun bytes(length: Int): ByteArray {
        val byteStream = ByteArrayOutputStream(length)
        val out = DataOutputStream(byteStream)
        for (i in 0 until length / 4) {
            out.writeInt(id)
        }
        out.flush()
        return byteStream.toByteArray()
    }
}