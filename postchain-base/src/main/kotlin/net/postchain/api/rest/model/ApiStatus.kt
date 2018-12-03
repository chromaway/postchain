package net.postchain.api.rest.model

import net.postchain.core.TransactionStatus

class ApiStatus(private val txStatus: TransactionStatus) {
    val status: String
        get() {
            return when (txStatus) {
                TransactionStatus.UNKNOWN -> "unknown"
                TransactionStatus.WAITING -> "waiting"
                TransactionStatus.CONFIRMED -> "confirmed"
                TransactionStatus.REJECTED -> "rejected"
            }
        }
}