package net.postchain.api.rest.controller

import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.ApiTx
import net.postchain.api.rest.model.TxRID
import net.postchain.base.ConfirmationProof

interface Model {
    fun postTransaction(tx: ApiTx)
    fun getTransaction(txRID: TxRID): ApiTx?
    fun getConfirmationProof(txRID: TxRID): ConfirmationProof?
    fun getStatus(txRID: TxRID): ApiStatus
    fun query(query: Query): QueryResult
}

data class Query(val json: String)
data class QueryResult(val json: String)

data class ErrorBody(val error: String = "")

class NotFoundError(message: String) : Exception(message)
class OverloadedException(message: String) : Exception(message)
