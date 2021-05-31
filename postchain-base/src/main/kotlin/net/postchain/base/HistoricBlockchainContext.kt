package net.postchain.base

import net.postchain.ebft.worker.WorkerContext
import net.postchain.network.x.XPeerID

class HistoricBlockchainContext(val historicBrid: BlockchainRid,
                                val ancestors: Map<BlockchainRid, Set<XPeerID>>) {
    lateinit var contextCreator: (BlockchainRid) -> WorkerContext


    /**
     * We want to use all ancestors we have when we look for blocks
     * Aliases are "sneaky links" used to pretend that some other blockchain is the one we need.
     *
     * @param myBRID is a BC RID we always wanna use, i.e. the "real" one
     * @return a list of alternative names
     */
    fun getChainsToSyncFrom(myBRID: BlockchainRid): List<BlockchainRid> {
        val chainsToSyncFrom = mutableListOf(
                myBRID,
                historicBrid
        )

        chainsToSyncFrom.addAll(ancestors.keys)
        return chainsToSyncFrom
    }
}