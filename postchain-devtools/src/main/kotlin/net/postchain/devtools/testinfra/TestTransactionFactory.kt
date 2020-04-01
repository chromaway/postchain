// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.testinfra

import mu.KLogging
import net.postchain.common.toHex
import net.postchain.core.Transaction
import net.postchain.core.TransactionFactory
import net.postchain.core.UserMistake
import java.io.DataInputStream

class TestTransactionFactory : TransactionFactory {

    val specialTxs = mutableMapOf<Int, Transaction>()

    companion object : KLogging()

    override fun decodeTransaction(data: ByteArray): Transaction {
        val stream = DataInputStream(data.inputStream())
        val id = stream.readInt()
        if (specialTxs.containsKey(DataInputStream(data.inputStream()).readInt())) {
            return specialTxs[id]!!
        }

        val result = TestTransaction(id)

        if (!result.getRawData().contentEquals(data)) {
            throw UserMistake("This is a test, and you must send TX data in a format so it can be interpreted " +
                  "as an integer. You sent: ${data.toHex()} (and the format we expected was ${result.getRawData().toHex()})")
        }

        return result
    }
}