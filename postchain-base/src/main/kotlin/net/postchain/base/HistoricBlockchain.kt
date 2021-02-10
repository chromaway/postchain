package net.postchain.base

import net.postchain.core.BlockQueries
import net.postchain.ebft.worker.WorkerContext

class HistoricBlockchain(val historicBrid: BlockchainRid) {
    lateinit var contextCreator: () -> WorkerContext
    var historicBlockQueries: BlockQueries? = null
}