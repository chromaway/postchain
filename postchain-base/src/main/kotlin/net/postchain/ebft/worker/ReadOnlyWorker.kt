// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.ebft.worker

import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockchainEngine
import net.postchain.core.NODE_ID_READ_ONLY
import net.postchain.core.RestartHandler
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.message.Message
import net.postchain.ebft.syncmanager.ReplicaSyncManager
import net.postchain.network.CommunicationManager

/**
 * A blockchain instance replica worker
 * @property updateLoop the main thread
 */
class ReadOnlyWorker(
        override val name: String,
        signers: List<ByteArray>,
        override val blockchainEngine: BlockchainEngine,
        val communicationManager: CommunicationManager<Message>,
        override val restartHandler: RestartHandler
) : AbstractBlockchainProcess() {

    override val blockDatabase: BaseBlockDatabase
    override val syncManager: ReplicaSyncManager
    override val networkAwareTxQueue: NetworkAwareTxQueue

    init {
        blockDatabase = BaseBlockDatabase(
                blockchainEngine, blockchainEngine.getBlockQueries(), NODE_ID_READ_ONLY)

        syncManager = ReplicaSyncManager(
                name,
                signers,
                communicationManager,
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