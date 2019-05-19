package net.postchain.client

import net.postchain.core.TransactionStatus

class ConfirmedTransaction : TransactionResult {
    override val status: TransactionStatus = TransactionStatus.CONFIRMED
}