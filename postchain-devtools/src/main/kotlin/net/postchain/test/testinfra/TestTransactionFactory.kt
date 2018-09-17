package net.postchain.test.testinfra

import net.postchain.core.Transaction
import net.postchain.core.TransactionFactory
import org.junit.Assert
import java.io.DataInputStream

class TestTransactionFactory : TransactionFactory {

    val specialTxs = mutableMapOf<Int, Transaction>()

    override fun decodeTransaction(data: ByteArray): Transaction {
        val id = DataInputStream(data.inputStream()).readInt()
        if (specialTxs.containsKey(DataInputStream(data.inputStream()).readInt())) {
            return specialTxs[id]!!
        }
        val result = TestTransaction(id)
        Assert.assertArrayEquals(result.getRawData(), data)
        return result
    }
}