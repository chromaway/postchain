// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base.data

import mu.KLogging
import net.postchain.core.*
import java.util.concurrent.LinkedBlockingQueue

class ComparableTransaction(val tx: Transaction) {
    override fun equals(other: Any?): Boolean {
        if (other is ComparableTransaction) {
            return tx.getRID().contentEquals(other.tx.getRID())
        }
        return false
    }

    override fun hashCode(): Int {
        return tx.getRID().hashCode()
    }
}

val MAX_REJECTED = 1000

/**
 * Transaction queue for transactions received from peers
 */
class BaseTransactionQueue(queueCapacity: Int = 2500) : TransactionQueue {

    companion object : KLogging()

    private val queue = LinkedBlockingQueue<ComparableTransaction>(queueCapacity)
    private val queueSet = HashSet<ByteArrayKey>()
    private val taken = mutableListOf<ComparableTransaction>()
    private val rejects = object : LinkedHashMap<ByteArrayKey, Exception?>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ByteArrayKey, java.lang.Exception?>?): Boolean {
            return size > MAX_REJECTED
        }
    }

    @Synchronized
    override fun takeTransaction(): Transaction? {
        val tx = queue.poll()
        return if (tx != null) {
            taken.add(tx)
            queueSet.remove(ByteArrayKey(tx.tx.getRID()))
            tx.tx
        } else null
    }

    override fun getTransactionQueueSize(): Int {
        return queue.size
    }

    override fun enqueue(tx: Transaction): TransactionResult {
        val rid = ByteArrayKey(tx.getRID())
        synchronized(this) {
            if (queueSet.contains(rid)) {
                logger.debug("Skipping $rid first test")
                return TransactionResult.DUPLICATE
            }
        }

        val comparableTx = ComparableTransaction(tx)
        try {
            if (tx.isCorrect()) {
                synchronized(this) {
                    if (queueSet.contains(rid)) {
                        logger.debug("Skipping $rid second test")
                        return TransactionResult.DUPLICATE
                    }
                    if (queue.offer(comparableTx)) {
                        logger.debug("Enqueued tx $rid")
                        queueSet.add(rid)
                        return TransactionResult.OK
                    } else {
                        logger.debug("Skipping tx $rid, overloaded. Queue contains ${queue.size} elements")
                        return TransactionResult.FULL
                    }
                }
            } else {
                logger.debug("Tx $rid didn't pass the check")
                rejectTransaction(tx, null)
                return TransactionResult.INVALID
            }

        } catch (e: UserMistake) {
            logger.debug("Tx $rid didn't pass the check: ${e.message}")
            rejectTransaction(tx, e)
        }

        return TransactionResult.UNKNOWN
    }

    @Synchronized
    override fun getTransactionStatus(txHash: ByteArray): TransactionStatus {
        val rid = ByteArrayKey(txHash)
        return when {
            rid in queueSet -> TransactionStatus.WAITING
            taken.find { it.tx.getRID().contentEquals(txHash) } != null -> TransactionStatus.WAITING
            rid in rejects -> TransactionStatus.REJECTED
            else -> TransactionStatus.UNKNOWN
        }
    }

    @Synchronized
    override fun rejectTransaction(tx: Transaction, reason: Exception?) {
        taken.remove(ComparableTransaction(tx))
        rejects[ByteArrayKey(tx.getRID())] = reason
    }

    @Synchronized
    override fun removeAll(transactionsToRemove: Collection<Transaction>) {
        queue.removeAll(transactionsToRemove.map { ComparableTransaction(it) })
        queueSet.removeAll(transactionsToRemove.map { ByteArrayKey(it.getRID()) })
        taken.removeAll(transactionsToRemove.map { ComparableTransaction(it) })
    }

    @Synchronized
    override fun getRejectionReason(txRID: ByteArrayKey): Exception? {
        return rejects[txRID]
    }
}