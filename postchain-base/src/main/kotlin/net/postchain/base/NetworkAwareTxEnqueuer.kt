// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import mu.KLogging
import net.postchain.common.toHex
import net.postchain.core.TransactionQueue
import net.postchain.core.TransactionResult
import net.postchain.ebft.message.Message
import net.postchain.network.CommunicationManager

/**
 * Transaction queue for transactions added locally via the REST API
 */
class NetworkAwareTxQueue(
        private val queue: TransactionQueue,
        private val network: CommunicationManager<Message>)
    : TransactionQueue by queue {

    companion object : KLogging()

    /*
These are the cases to take care of:

1. I'm currently primary and haven't done buildBlock() yet
2. I'm currently primary and I have done buildBlock()
3. I'm not primary

Primary may change and buildBlock() may start while tx is in transit.

I started by making a decorator on TransactionEnqueuer called NetworkAwareTransactionEnqueuer that
forwards to current primary if I'm not primary or if I'm primary and buildBlock() has started.
That class needs to know a lot of stuff:

* If we are currently primary
* If buildBlock has been started
* Who the next primary is

Then extra complexities are added if nodes goes down.

I ended up opting for this solution instead:

* Enqueue locally AND broadcast tx (we could possibly limit broadcasting to 2f nodes?)
* When block is committed, the transactions in the block are removed from queue, if present.

This has a few drawbacks:

* DoS attacks becomes easy. Correct (isCorrect() == true) transactions that will fail during
apply(), can be created en-masse to fill up the "mempools". We have no way to control
this but to throttle rate of incoming transactions prior to queueing.
* High bandwidth requirement

Despite these drawbacks, I think this is the way to go for now. I haven't found another model
where we are guaranteed not to drop transactions.
 */

    override fun enqueue(tx: net.postchain.core.Transaction): TransactionResult {
        val result = queue.enqueue(tx)
        if (result == TransactionResult.OK) {
            logger.debug("Node broadcasting tx ${tx.getRID().toHex()}")
            network.broadcastPacket(net.postchain.ebft.message.Transaction(tx.getRawData()))
        }
        return result
    }
}