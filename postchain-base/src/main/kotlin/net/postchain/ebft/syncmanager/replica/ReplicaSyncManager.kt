package net.postchain.ebft.syncmanager.replica

import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.NodeStateTracker
import net.postchain.debug.BlockchainProcessName
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.message.Message
import net.postchain.ebft.rest.contract.serialize
import net.postchain.ebft.syncmanager.SyncManager
import net.postchain.ebft.syncmanager.common.FastSynchronizer
import net.postchain.network.CommunicationManager

class ReplicaSyncManager(
        processName: BlockchainProcessName,
        signers: List<ByteArray>,
        communicationManager: CommunicationManager<Message>,
        private val nodeStateTracker: NodeStateTracker,
        blockDatabase: BlockDatabase,
        val blockQueries: BlockQueries,
        blockchainConfiguration: BlockchainConfiguration
) : SyncManager {

    private val fastSynchronizer = FastSynchronizer(
            processName,
            signers,
            communicationManager,
            blockDatabase,
            blockchainConfiguration,
            blockQueries
    )

    override fun update() {
        fastSynchronizer.logCurrentState()
        nodeStateTracker.blockHeight = fastSynchronizer.blockHeight
        nodeStateTracker.nodeStatuses = fastSynchronizer.nodeStatuses().map { it.serialize() }.toTypedArray()
        fastSynchronizer.sync()
    }
}
