// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

/**
 * Transactor is an individual operation which can be applied to the database
 * Transaction might consist of one or more operations
 * Transaction should be serializable, but transactor doesn't need to have a serialized
 * representation as we only care about storing of the whole Transaction
 */
interface Transactor {
    // special transactions cannot be added to a transaction queue,
    // they can only be appended directly by blockchain engine
    fun isSpecial(): Boolean

    fun isCorrect(): Boolean
    fun apply(ctx: TxEContext): Boolean
}

interface Transaction : Transactor {
    fun getRawData(): ByteArray
    fun getRID(): ByteArray  // transaction unique identifier which is used as a reference to it
    fun getHash(): ByteArray // hash of transaction content
}

enum class TransactionStatus(val status: String) {
    UNKNOWN("unknown"),
    WAITING("waiting"),
    CONFIRMED("confirmed"),
    REJECTED("rejected")
}

enum class TransactionResult(val code: Int) {
    OK(0),
    FULL(1),
    DUPLICATE(2),
    INVALID(3),
    UNKNOWN(9999)
}

interface TransactionFactory {
    fun decodeTransaction(data: ByteArray): Transaction
}

interface TransactionQueue {
    fun takeTransaction(): Transaction?
    fun enqueue(tx: Transaction): TransactionResult
    fun findTransaction(txRID: ByteArrayKey): Transaction?
    fun getTransactionStatus(txHash: ByteArray): TransactionStatus
    fun getTransactionQueueSize(): Int
    fun removeAll(transactionsToRemove: Collection<Transaction>)
    fun rejectTransaction(tx: Transaction, reason: Exception?)
    fun getRejectionReason(txRID: ByteArrayKey): Exception?
}


