package net.postchain.ebft.rest.model

import net.postchain.api.rest.controller.*
import net.postchain.api.rest.json.JsonFactory
import net.postchain.base.BaseBlockQueries
import net.postchain.core.NodeStateTracker
import net.postchain.core.TransactionFactory
import net.postchain.core.TransactionQueue

class PostchainEBFTModel(
        private val nodeStateTracker: NodeStateTracker,
        txQueue: TransactionQueue,
        transactionFactory: TransactionFactory,
        blockQueries: BaseBlockQueries,
        debugInfoQuery: DebugInfoQuery
) : PostchainModel(txQueue, transactionFactory, blockQueries, debugInfoQuery) {

    override fun nodeQuery(subQuery: String): String {
        val json = JsonFactory.makeJson()
        return when (subQuery) {
            "height" -> json.toJson(BlockHeight(nodeStateTracker.blockHeight))
            "my_status" -> nodeStateTracker.myStatus ?: throw NotFoundError("NotFound")
            "statuses" -> nodeStateTracker.nodeStatuses?.joinToString(separator = ",", prefix = "[", postfix = "]")
                    ?: throw NotFoundError("NotFound")
            else -> throw NotSupported("NotSupported: $subQuery")
        }
    }
}
