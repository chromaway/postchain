// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.api.rest.controller

import mu.KLogging
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.ApiTx
import net.postchain.api.rest.model.TxRID
import net.postchain.base.BaseBlockQueries
import net.postchain.base.ConfirmationProof
import net.postchain.common.TimeLog
import net.postchain.common.toHex
import net.postchain.core.TransactionFactory
import net.postchain.core.TransactionQueue
import net.postchain.core.TransactionStatus.CONFIRMED
import net.postchain.core.TransactionStatus.UNKNOWN
import net.postchain.core.UserMistake

open class PostchainModel(
        val txQueue: TransactionQueue,
        private val transactionFactory: TransactionFactory,
        val blockQueries: BaseBlockQueries
) : Model {
    companion object : KLogging()

    override fun postTransaction(tx: ApiTx) {
        var nonce = TimeLog.startSumConc("PostchainModel.postTransaction().decodeTransaction")
        val decodedTransaction = transactionFactory.decodeTransaction(tx.bytes)
        TimeLog.end("PostchainModel.postTransaction().decodeTransaction", nonce)

        nonce = TimeLog.startSumConc("PostchainModel.postTransaction().isCorrect")
        if (!decodedTransaction.isCorrect()) {
            throw UserMistake("Transaction ${decodedTransaction.getRID()} is not correct")
        }
        TimeLog.end("PostchainModel.postTransaction().isCorrect", nonce)
        nonce = TimeLog.startSumConc("PostchainModel.postTransaction().enqueue")
        if (!txQueue.enqueue(decodedTransaction))
            throw OverloadedException("Transaction queue is full")
        TimeLog.end("PostchainModel.postTransaction().enqueue", nonce)
    }

    override fun getTransaction(txRID: TxRID): ApiTx? {
        return blockQueries.getTransaction(txRID.bytes).get()
                .takeIf { it != null }
                ?.let { ApiTx(it.getRawData().toHex()) }
    }

    override fun getConfirmationProof(txRID: TxRID): ConfirmationProof? {
        return blockQueries.getConfirmationProof(txRID.bytes).get()
    }

    override fun getStatus(txRID: TxRID): ApiStatus {
        var status = txQueue.getTransactionStatus(txRID.bytes)

        if (status == UNKNOWN) {
            status = if (blockQueries.isTransactionConfirmed(txRID.bytes).get())
                CONFIRMED else UNKNOWN
        }

        return ApiStatus(status)
    }

    override fun query(query: Query): QueryResult {
        return QueryResult(blockQueries.query(query.json).get())
    }
}