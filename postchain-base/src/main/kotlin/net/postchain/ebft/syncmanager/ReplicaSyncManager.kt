package net.postchain.ebft.syncmanager

import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.NodeStateTracker
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.message.Message
import net.postchain.ebft.rest.contract.serialize
import net.postchain.ebft.syncmanager.replica.FastSyncAlgorithm
import net.postchain.ebft.syncmanager.replica.IncomingBlock
import net.postchain.ebft.syncmanager.replica.IssuedRequestTimer
import net.postchain.ebft.syncmanager.replica.ReplicaTelemetry
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID
import java.util.*

class ReplicaSyncManager(
        private val blockchainProcessName: String,
        signers: List<ByteArray>,
        private val communicationManager: CommunicationManager<Message>,
        private val nodeStateTracker: NodeStateTracker,
        blockDatabase: BlockDatabase,
        val blockQueries: BlockQueries,
        blockchainConfiguration: BlockchainConfiguration
) : SyncManagerBase {

    private val parallelism = 10
    private val nodePoolCount = 2 // used to select 1 random node
    private val defaultBackoffDelta = 1000
    private val maxBackoffDelta = 30 * defaultBackoffDelta
    private val validatorNodes: List<XPeerID> = signers.map { XPeerID(it) }
    private val replicaLogger = ReplicaTelemetry(blockchainProcessName)

    private var blockHeight: Long = blockQueries.getBestHeight().get()
    private var nodesWithBlocks = hashMapOf<XPeerID, Long>()
    private var parallelRequestsState = hashMapOf<Long, IssuedRequestTimer>()
    private var blocks = PriorityQueue<IncomingBlock>(parallelism)
    private val fastSyncAlgorithm = FastSyncAlgorithm(
            blockchainProcessName,
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
