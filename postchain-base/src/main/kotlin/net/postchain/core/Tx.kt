package net.postchain.core

/**
 * Transactor is an individual operation which can be applied to the database
 * Transaction might consist of one or more operations
 * Transaction should be serializable, but transactor doesn't need to have a serialized
 * representation as we only care about storing of the whole Transaction
 */
interface Transactor {
    fun isCorrect(): Boolean
    fun apply(ctx: TxEContext): Boolean
}

interface Transaction : Transactor {
    fun getRawData(): ByteArray
    fun getRID(): ByteArray  // transaction unique identifier which is used as a reference to it
    fun getHash(): ByteArray // hash of transaction content
}

enum class TransactionStatus {
    UNKNOWN, REJECTED, WAITING, CONFIRMED
}

interface TransactionFactory {
    fun decodeTransaction(data: ByteArray): Transaction
}

interface TransactionQueue {
    fun takeTransaction(): Transaction?
    fun enqueue(tx: Transaction): Boolean
    fun getTransactionStatus(txHash: ByteArray): TransactionStatus
    fun getTransactionQueueSize(): Int
    fun removeAll(transactionsToRemove: Collection<Transaction>)
    fun rejectTransaction(tx: Transaction, reason: Exception?)
}


