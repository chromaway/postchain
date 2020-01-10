package net.postchain.ebft.syncmanager

import net.postchain.core.BlockDataWithWitness
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.NodeStateTracker
import net.postchain.debug.BlockchainProcessName
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.message.Message
import net.postchain.ebft.rest.contract.serialize
import net.postchain.ebft.syncmanager.replica.FastSyncAlgorithm
import net.postchain.network.CommunicationManager

class ReplicaSyncManager(
        private val processName: BlockchainProcessName,
        signers: List<ByteArray>,
        private val communicationManager: CommunicationManager<Message>,
        private val nodeStateTracker: NodeStateTracker,
        blockDatabase: BlockDatabase,
        val blockQueries: BlockQueries,
        blockchainConfiguration: BlockchainConfiguration
) : SyncManagerBase {

    private val fastSyncAlgorithm = FastSyncAlgorithm(
            processName,
            signers,
            communicationManager,
            blockDatabase,
            blockchainConfiguration,
            blockQueries
    )

    override fun update() {
        fastSyncAlgorithm.logCurrentState()
        nodeStateTracker.blockHeight = fastSyncAlgorithm.blockHeight
        nodeStateTracker.nodeStatuses = fastSyncAlgorithm.nodeStatuses().map { it.serialize() }.toTypedArray()
        fastSyncAlgorithm.sync()
    }
}
