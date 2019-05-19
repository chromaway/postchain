package net.postchain.client

import net.postchain.core.TransactionStatus

class RejectedTransaction : TransactionResult {
    override val status: TransactionStatus = TransactionStatus.REJECTED
}