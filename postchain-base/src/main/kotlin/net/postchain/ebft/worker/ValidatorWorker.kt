package net.postchain.ebft.worker

import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockchainEngine
import net.postchain.core.NodeStateTracker
import net.postchain.core.RestartHandler
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.BaseBlockManager
import net.postchain.ebft.BaseStatusManager
import net.postchain.ebft.BlockManager
import net.postchain.ebft.message.Message
import net.postchain.ebft.syncmanager.ValidatorSyncManager
import net.postchain.network.CommunicationManager

/**
 * A blockchain instance worker
 *
 * @property updateLoop the main thread
 * @property blockManager manages intents and acts as a wrapper for [blockDatabase] and [statusManager]
 * @property statusManager manages the status of the consensus protocol
 */
class ValidatorWorker(
        override val name: String,
        signers: List<ByteArray>,
        override val blockchainEngine: BlockchainEngine,
        nodeIndex: Int,
        private val communicationManager: CommunicationManager<Message>,
        override val restartHandler: RestartHandler
) : AbstractBlockchainProcess() {

    override val blockDatabase: BaseBlockDatabase
    private val blockManager: BlockManager
    val statusManager: BaseStatusManager
    override val syncManager: ValidatorSyncManager
    override val networkAwareTxQueue: NetworkAwareTxQueue
    override val nodeStateTracker = NodeStateTracker()

    init {
        val bestHeight = blockchainEngine.getBlockQueries().getBestHeight().get()
        statusManager = BaseStatusManager(
                signers.size,
                nodeIndex,
                bestHeight + 1)

        blockDatabase = BaseBlockDatabase(
                blockchainEngine, blockchainEngine.getBlockQueries(), nodeIndex)

        blockManager = BaseBlockManager(
                blockDatabase,
                statusManager,
                blockchainEngine.getBlockBuildingStrategy())

        // Give the SyncManager the BaseTransactionQueue and not the network-aware one,
        // because we don't want tx forwarding/broadcasting when received through p2p network
        syncManager = ValidatorSyncManager(
                name,
                signers,
                statusManager,
                blockManager,
                blockDatabase,
                communicationManager,
                nodeStateTracker,
                blockchainEngine.getTransactionQueue(),
                blockchainEngine.getConfiguration())

        networkAwareTxQueue = NetworkAwareTxQueue(
                blockchainEngine.getTransactionQueue(),
                communicationManager)

        statusManager.recomputeStatus()
        startUpdateLoop(syncManager)
    }


    /**
     * Stop the postchain node
     */
    override fun shutdown() {
        super.shutdown()
        communicationManager.shutdown()
    }
}