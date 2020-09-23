// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.controller

import mu.KLogging
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.ApiTx
import net.postchain.api.rest.model.TxRID
import net.postchain.base.BaseBlockQueries
import net.postchain.base.ConfirmationProof
import net.postchain.common.TimeLog
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.core.TransactionStatus.*
import net.postchain.gtv.Gtv

open class PostchainModel(
        override val chainIID: Long,
        val txQueue: TransactionQueue,
        private val transactionFactory: TransactionFactory,
        val blockQueries: BaseBlockQueries,
        private val debugInfoQuery: DebugInfoQuery
) : Model {

    companion object : KLogging()

    override fun postTransaction(tx: ApiTx) {
        var nonce = TimeLog.startSumConc("PostchainModel.postTransaction().decodeTransaction")
        val decodedTransaction = transactionFactory.decodeTransaction(tx.bytes)
        TimeLog.end("PostchainModel.postTransaction().decodeTransaction", nonce)

        nonce = TimeLog.startSumConc("PostchainModel.postTransaction().isCorrect")
        if (!decodedTransaction.isCorrect()) {
            throw UserMistake("Transaction ${decodedTransaction.getRID().toHex()} is not correct")
        }
        TimeLog.end("PostchainModel.postTransaction().isCorrect", nonce)
        nonce = TimeLog.startSumConc("PostchainModel.postTransaction().enqueue")
        when (txQueue.enqueue(decodedTransaction)) {
            TransactionResult.FULL -> throw OverloadedException("Transaction queue is full")
            TransactionResult.INVALID -> throw InvalidTnxException("Transaction is invalid")
            TransactionResult.DUPLICATE -> throw DuplicateTnxException("Transaction already in queue")
            TransactionResult.UNKNOWN -> throw UserMistake("Unknown error")
            else -> {}
        }
        TimeLog.end("PostchainModel.postTransaction().enqueue", nonce)
    }

    override fun getTransaction(txRID: TxRID): ApiTx? {
        return blockQueries.getTransaction(txRID.bytes).get()
                .takeIf { it != null }
                ?.let { ApiTx(it.getRawData().toHex()) }
    }

    override fun getTransactionInfo(txRID: TxRID): TransactionInfoExt? {
        return blockQueries.getTransactionInfo(txRID.bytes).get()
    }
    override fun getTransactionsInfo(beforeTime: Long, limit: Int): List<TransactionInfoExt> {
        return blockQueries.getTransactionsInfo(beforeTime, limit).get()
    }

    override fun getBlocks(beforeTime: Long, limit: Int, partialTx: Boolean): List<BlockDetail> {
        return blockQueries.getBlocks(beforeTime, limit, partialTx).get()
    }

    override fun getBlock(blockRID: ByteArray, partialTx: Boolean): BlockDetail? {
        return blockQueries.getBlock(blockRID, partialTx).get()
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

        return if (status == REJECTED) {
            val exception = txQueue.getRejectionReason(txRID.bytes.byteArrayKeyOf())
            ApiStatus(status, exception?.message)
        } else {
            ApiStatus(status)
        }
    }

    override fun query(query: Query): QueryResult {
        return QueryResult(blockQueries.query(query.json).get())
    }

    override fun query(query: Gtv): Gtv {
        return blockQueries.query(query[0].asString(), query[1]).get()
    }

    override fun nodeQuery(subQuery: String): String = throw NotSupported("NotSupported: $subQuery")

    override fun debugQuery(subQuery: String?): String {
        return debugInfoQuery.queryDebugInfo(subQuery)
    }
}
