package net.postchain.client

import net.postchain.core.TransactionStatus

class TransactionResultImpl(override val status: TransactionStatus) : TransactionResult {

}