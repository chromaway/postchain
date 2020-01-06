package net.postchain.ebft.syncmanager

import net.postchain.core.BlockDataWithWitness
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.NodeStateTracker
import net.postchain.debug.BlockchainProcessName
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.NodeStatus
import net.postchain.ebft.message.CompleteBlock
import net.postchain.ebft.message.GetBlockAtHeight
import net.postchain.ebft.message.Message
import net.postchain.ebft.message.Status
import net.postchain.ebft.rest.contract.serialize
import net.postchain.ebft.syncmanager.replica.IncomingBlock
import net.postchain.ebft.syncmanager.replica.IssuedRequestTimer
import net.postchain.ebft.syncmanager.replica.ReplicaTelemetry
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID
import java.lang.Integer.min
import java.util.*

class ReplicaSyncManager(
        private val processName: BlockchainProcessName,
        signers: List<ByteArray>,
        private val communicationManager: CommunicationManager<Message>,
        private val nodeStateTracker: NodeStateTracker,
        private val blockDatabase: BlockDatabase,
        val blockQueries: BlockQueries,
        private val blockchainConfiguration: BlockchainConfiguration
) : SyncManagerBase {

    private val parallelism = 10
    private val nodePoolCount = 1 // used to select 1 random node
    private val defaultBackoffDelta = 1000
    private val maxBackoffDelta = 30 * defaultBackoffDelta
    private val validatorNodes: List<XPeerID> = signers.map { XPeerID(it) }
    private val replicaLogger = ReplicaTelemetry(processName)

    private var blockHeight: Long = blockQueries.getBestHeight().get()
    private var nodesWithBlocks = hashMapOf<XPeerID, Long>()
    private var parallelRequestsState = hashMapOf<Long, IssuedRequestTimer>()
    private var blocks = PriorityQueue<IncomingBlock>(parallelism)

    override fun update() {
        replicaLogger.logCurrentState(blockHeight, parallelRequestsState, blocks)
        nodeStateTracker.blockHeight = blockHeight
        nodeStateTracker.nodeStatuses = replicaLogger.nodeStatuses().map { it.serialize() }.toTypedArray()
        checkBlock()
        processState()
        dispatchMessages()
    }

    private fun checkBlock() {
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

    private fun commitBlock(block: BlockDataWithWitness) {
        blockDatabase.addBlock(block)
                .run {
                    success {
                        replicaLogger.blockAppendedToDatabase(blockHeight)
                        parallelRequestsState.remove(blockHeight)
                        blockHeight += 1
                        checkBlock()
                    }
                    fail {
                        replicaLogger.failedToAppendBlockToDatabase(blockHeight, it.message)
                        it.printStackTrace()
                    }
                }
    }

    private fun processState() {
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

    private fun askForBlock(height: Long) {
        nodesWithBlocks
                .filter { it.value >= height }
                .map { it.key }
                .toMutableList()
                .also {
                    it.shuffle()
                    if (it.count() >= nodePoolCount) {
                        replicaLogger.askForBlock(height, blockHeight)
                        val timer = parallelRequestsState[height]
                                ?: IssuedRequestTimer(defaultBackoffDelta, Date().time)
                        val backoffDelta = min((timer.backoffDelta.toDouble() * 1.1).toInt(), maxBackoffDelta)
                        communicationManager.sendPacket(GetBlockAtHeight(height), it.first())
                        parallelRequestsState[height] = timer.copy(backoffDelta = backoffDelta, lastSentTimestamp = Date().time)
                    }
                }
    }

    private fun doesQueueContainsBlock(height: Long) = blocks.firstOrNull { it.height == height } != null

    private fun dispatchMessages() {
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
                        replicaLogger.reportNodeStatus(index, nodeStatus)

                        if (xPeerId in validatorNodes) {
                            nodesWithBlocks[xPeerId] = message.height - 1
                        }
                    }

                    else -> replicaLogger.debug("Unhandled type ${message::class}")
                }
            } catch (e: Exception) {
                replicaLogger.fatal("Couldn't handle message $message. Ignoring and continuing", e)
            }
        }
    }
}
