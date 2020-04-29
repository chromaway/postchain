package net.postchain.integrationtest.statemachine

import mu.KLogging
import net.postchain.core.Transaction
import net.postchain.core.TransactionFactory
import net.postchain.devtools.testinfra.TestTransaction
import java.io.DataInputStream

/**
 * A TransactionFactory which decodes Transaction as incorrect one iff `id` is an odd number and `failable` == true.
 */
open class FailableTestTransactionFactory(private val failable: Boolean) : TransactionFactory {

    companion object : KLogging()

    override fun decodeTransaction(data: ByteArray): Transaction {
        val stream = DataInputStream(data.inputStream())
        val id = stream.readInt()

        return TestTransaction(
                id = id,
                correct = !failable || (id % 2 == 0))
    }
}

/**
 * A TransactionFactory which decodes Transaction as incorrect one iff `id` is an odd number.
 */
class FailedTestTransactionFactory : FailableTestTransactionFactory(true)

/**
 * A TransactionFactory which decodes Transaction as correct (see `FailedTestTransactionFactory`).
 */
class NotFailedTestTransactionFactory : FailableTestTransactionFactory(false)