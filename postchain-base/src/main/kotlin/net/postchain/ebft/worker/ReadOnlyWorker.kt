// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.worker

import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockchainEngine
import net.postchain.core.NODE_ID_READ_ONLY
import net.postchain.core.NodeStateTracker
import net.postchain.debug.BlockchainProcessName
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.message.Message
import net.postchain.ebft.syncmanager.SyncManager
import net.postchain.ebft.syncmanager.replica.ReplicaSyncManager
import net.postchain.network.CommunicationManager

/**
 * A blockchain instance replica worker
 * @property updateLoop the main thread
 */
class ReadOnlyWorker(
        override val processName: BlockchainProcessName,
        signers: List<ByteArray>,
        override val blockchainEngine: BlockchainEngine,
        private val communicationManager: CommunicationManager<Message>
) : AbstractBlockchainProcess() {

    override val blockDatabase: BaseBlockDatabase
    override val syncManager: SyncManager
    override val networkAwareTxQueue: NetworkAwareTxQueue
    override val nodeStateTracker = NodeStateTracker()

    init {
        blockDatabase = BaseBlockDatabase(
                blockchainEngine, blockchainEngine.getBlockQueries(), NODE_ID_READ_ONLY)

        syncManager = ReplicaSyncManager(
                processName,
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

    override fun shutdown() {
        super.shutdown()
        communicationManager.shutdown()
    }
}