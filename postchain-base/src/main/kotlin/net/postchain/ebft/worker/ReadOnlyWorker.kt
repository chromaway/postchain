// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.ebft.worker

import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockchainEngine
import net.postchain.core.NODE_ID_READ_ONLY
import net.postchain.core.NodeStateTracker
import net.postchain.core.RestartHandler
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.syncmanager.ReplicaSyncManager
import net.postchain.network.CommunicationManager

/**
 * A blockchain instance replica worker
 * @property updateLoop the main thread
 */
class ReadOnlyWorker(
        signers: List<ByteArray>,
        override val blockchainEngine: BlockchainEngine,
        communicationManager: CommunicationManager<EbftMessage>,
        override val restartHandler: RestartHandler
) : AbstractBlockchainProcess() {

    override val blockDatabase: BaseBlockDatabase
    override val syncManager: ReplicaSyncManager
    override val networkAwareTxQueue: NetworkAwareTxQueue
    override val nodeStateTracker = NodeStateTracker()

    init {
        blockDatabase = BaseBlockDatabase(
                blockchainEngine, blockchainEngine.getBlockQueries(), NODE_ID_READ_ONLY)

        syncManager = ReplicaSyncManager(
                signers,
                communicationManager,
                nodeStateTracker,
                blockDatabase,
                blockchainEngine.getBlockQueries(),
                blockchainEngine.getConfiguration())

        networkAwareTxQueue = NetworkAwareTxQueue(
                blockchainEngine.getTransactionQueue(),
                communicationManager)

        startUpdateLoop(syncManager)
    }
}