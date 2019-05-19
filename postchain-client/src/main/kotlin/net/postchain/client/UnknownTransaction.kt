package net.postchain.client

import net.postchain.core.TransactionStatus

class UnknownTransaction : TransactionResult {
    override val status: TransactionStatus = TransactionStatus.UNKNOWN
}