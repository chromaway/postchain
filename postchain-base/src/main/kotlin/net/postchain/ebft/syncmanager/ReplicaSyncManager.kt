package net.postchain.ebft.syncmanager

import net.postchain.core.*
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.NodeStatus
import net.postchain.ebft.message.CompleteBlock
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.message.GetBlockAtHeight
import net.postchain.ebft.message.Status
import net.postchain.ebft.rest.contract.serialize
import net.postchain.ebft.syncmanager.replica.IncomingBlock
import net.postchain.ebft.syncmanager.replica.IssuedRequestTimer
import net.postchain.ebft.syncmanager.replica.ReplicaTelemetry
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*

class FastSyncAlgorithm(
        signers: List<ByteArray>,
        private val communicationManager: CommunicationManager<EbftMessage>,
        private val blockDatabase: BlockDatabase,
        private val blockchainConfiguration: BlockchainConfiguration,
        private val replicaTelemetry: ReplicaTelemetry,
        val blockQueries: BlockQueries
)
{
    private val validatorNodes: List<XPeerID> = signers.map { XPeerID(it) }
    private val parallelism = 10
    private val nodePoolCount = max(1, signers.count() / 2)
    private val defaultBackoffDelta = 1000
    private val maxBackoffDelta = 30 * defaultBackoffDelta

    private var nodesWithBlocks = hashMapOf<XPeerID, Long>()
    var blockHeight: Long = blockQueries.getBestHeight().get()
        private set
    var parallelRequestsState = hashMapOf<Long, IssuedRequestTimer>()
        private set
    var blocks = PriorityQueue<IncomingBlock>(parallelism)
        private set

    private fun doesQueueContainsBlock(height: Long) = blocks.firstOrNull { it.height == height } != null

    fun checkBlock() {
        if (blocks.peek() != null) {
            blocks.peek().height.let {
                when {
                    it <= blockHeight -> blocks.remove()
                    it == blockHeight + 1 -> commitBlock(blocks.remove().block)
                    else -> Unit
                }
            }
        }
    }

    fun processState() {
        parallelRequestsState.entries.removeIf { it.key <= blockHeight }
        val maxElem = (parallelRequestsState.maxBy { it.key }?.key ?: blockHeight) + 1
        val diff = parallelism - parallelRequestsState.count()
        (maxElem until maxElem + diff).subtract(parallelRequestsState.keys).map { askForBlock(it) }

        parallelRequestsState.entries.associate {
            val state = IssuedRequestTimer(it.value.backoffDelta, it.value.lastSentTimestamp)
            if (!doesQueueContainsBlock(it.key) && Date().time > state.lastSentTimestamp + state.backoffDelta) {
                askForBlock(it.key)
            }
            it.key to state
        }
    }

    fun dispatchMessages() {
        for (packet in communicationManager.getPackets()) {
            val xPeerId = packet.first
            val message = packet.second
            try {
                when (message) {
                    is CompleteBlock -> {
                        if (!doesQueueContainsBlock(message.height) && message.height > blockHeight) {
                            blocks.offer(
                                    IncomingBlock(
                                            decodeBlockDataWithWitness(message, blockchainConfiguration),
                                            message.height)
                            )
                        }
                    }
                    is Status -> {
                        val nodeStatus = NodeStatus(message.height, message.serial)
                        val index = validatorNodes.indexOf(xPeerId)
                        replicaTelemetry.reportNodeStatus(index, nodeStatus)

                        if (xPeerId in validatorNodes) {
                            nodesWithBlocks[xPeerId] = message.height - 1
                        }
                    }
                    else -> throw ProgrammerMistake("Unhandled type ${message::class}")
                }
            } catch (e: Exception) {
                replicaTelemetry.fatal("Couldn't handle message $message. Ignoring and continuing", e)
            }
        }
    }

    private fun commitBlock(block: BlockDataWithWitness) {
        blockDatabase.addBlock(block)
                .run {
                    success {
                        replicaTelemetry.blockAppendedToDatabase(blockHeight)
                        parallelRequestsState.remove(blockHeight)
                        blockHeight += 1
                        checkBlock()
                    }
                    fail {
                        replicaTelemetry.failedToAppendBlockToDatabase(blockHeight, it.message)
                        it.printStackTrace()
                    }
                }
    }

    private fun askForBlock(height: Long) {
        nodesWithBlocks
                .filter { it.value >= height }
                .map { it.key }
                .toMutableList()
                .also {
                    it.shuffle()
                    if (it.count() >= nodePoolCount) {
                        replicaTelemetry.askForBlock(height, blockHeight)
                        val timer = parallelRequestsState[height]
                                ?: IssuedRequestTimer(defaultBackoffDelta, Date().time)
                        val backoffDelta = min((timer.backoffDelta.toDouble() * 1.1).toInt(), maxBackoffDelta)
                        communicationManager.sendPacket(GetBlockAtHeight(height), it.first())
                        parallelRequestsState[height] = timer.copy(backoffDelta = backoffDelta, lastSentTimestamp = Date().time)
                    }
                }
    }
}

class ReplicaSyncManager(
        signers: List<ByteArray>,
        communicationManager: CommunicationManager<EbftMessage>,
        private val nodeStateTracker: NodeStateTracker,
        blockDatabase: BlockDatabase,
        val blockQueries: BlockQueries,
        blockchainConfiguration: BlockchainConfiguration
) : SyncManagerBase {

    private val replicaTelemetry = ReplicaTelemetry()
    private val fastSyncAlgorithm = FastSyncAlgorithm(
            signers,
            communicationManager,
            blockDatabase,
            blockchainConfiguration,
            replicaTelemetry,
            blockQueries
    )

    override fun update() {
        replicaTelemetry.logCurrentState(fastSyncAlgorithm.blockHeight, fastSyncAlgorithm.parallelRequestsState, fastSyncAlgorithm.blocks)

        nodeStateTracker.blockHeight = fastSyncAlgorithm.blockHeight
        nodeStateTracker.nodeStatuses = replicaTelemetry.nodeStatuses().map { it.serialize() }.toTypedArray()

        fastSyncAlgorithm.checkBlock()
        fastSyncAlgorithm.processState()
        fastSyncAlgorithm.dispatchMessages()
    }
}
