// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.model

import net.postchain.core.TransactionStatus

class ApiStatus(txStatus: TransactionStatus, val rejectReason: String? = null) {

    val status: String = txStatus.status

    init {
        if (txStatus != TransactionStatus.REJECTED) {
            check(rejectReason == null) {
                "rejectReason field can only be used with status: REJECTED"
            }
        }
    }
}