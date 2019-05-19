package net.postchain.client

import net.postchain.core.TransactionStatus

class WaitingTransaction : TransactionResult {
    override val status: TransactionStatus = TransactionStatus.WAITING
}