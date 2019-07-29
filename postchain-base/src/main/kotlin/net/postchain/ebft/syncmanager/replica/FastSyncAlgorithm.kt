package net.postchain.ebft.syncmanager.replica

import net.postchain.core.BlockDataWithWitness
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.ProgrammerMistake
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.NodeStatus
import net.postchain.ebft.message.CompleteBlock
import net.postchain.ebft.message.GetBlockAtHeight
import net.postchain.ebft.message.Message
import net.postchain.ebft.message.Status
import net.postchain.ebft.syncmanager.decodeBlockDataWithWitness
import net.postchain.ebft.syncmanager.replica.ReplicaTelemetry
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*

class FastSyncAlgorithm(
        private val blockchainProcessName: String,
        signers: List<ByteArray>,
        private val communicationManager: CommunicationManager<Message>,
        private val blockDatabase: BlockDatabase,
        private val blockchainConfiguration: BlockchainConfiguration,
        private val blockQueries: BlockQueries
)
{
    private val fastSyncAlgorithmTelemetry = ReplicaTelemetry(blockchainProcessName)
    private val validatorNodes: List<XPeerID> = signers.map { XPeerID(it) }
    private val parallelism = 10
    private val nodePoolCount: Int = max(1, signers.count() / 2)
    private val defaultBackoffDelta = 1000
    private val maxBackoffDelta = 30 * defaultBackoffDelta

    private var nodesWithBlocks = hashMapOf<XPeerID, Long>()
    private var parallelRequestsState = hashMapOf<Long, IssuedRequestTimer>()
    private var blocks = PriorityQueue<IncomingBlock>(parallelism)

    val blockHeightAheadCount = 3

    var blockHeight: Long = blockQueries.getBestHeight().get()
        private set

    private fun doesQueueContainsBlock(height: Long) = blocks.firstOrNull { it.height == height } != null

    fun sync() {
        checkBlock()
        processState()
        dispatchMessages()
    }

    fun isUpToDate(): Boolean {
        val highest = nodeStatuses().map { it.height }.max()?: Long.MAX_VALUE
        return if((highest - blockHeight) > blockHeightAheadCount) {
            false
        } else {
            parallelRequestsState.clear()
            blocks.clear()
            true
        }
    }

    fun nodeStatuses() = fastSyncAlgorithmTelemetry.nodeStatuses()

    fun logCurrentState() = fastSyncAlgorithmTelemetry.logCurrentState(blockHeight, parallelRequestsState, blocks)

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
                        fastSyncAlgorithmTelemetry.reportNodeStatus(index, nodeStatus)

                        if (xPeerId in validatorNodes) {
                            nodesWithBlocks[xPeerId] = message.height - 1
                        }
                    }
                    else -> throw ProgrammerMistake("Unhandled type ${message::class}")
                }
            } catch (e: Exception) {
                fastSyncAlgorithmTelemetry.fatal("Couldn't handle message $message. Ignoring and continuing", e)
            }
        }
    }

    private fun commitBlock(block: BlockDataWithWitness) {
        blockDatabase.addBlock(block)
                .run {
                    success {
                        fastSyncAlgorithmTelemetry.blockAppendedToDatabase(blockHeight)
                        parallelRequestsState.remove(blockHeight)
                        blockHeight += 1
                        checkBlock()
                    }
                    fail {
                        parallelRequestsState.remove(blockHeight)
                        blockHeight = blockQueries.getBestHeight().get()
                        fastSyncAlgorithmTelemetry.failedToAppendBlockToDatabase(blockHeight, it.message)
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
                        fastSyncAlgorithmTelemetry.askForBlock(height, blockHeight)
                        val timer = parallelRequestsState[height]
                                ?: IssuedRequestTimer(defaultBackoffDelta, Date().time)
                        val backoffDelta = min((timer.backoffDelta.toDouble() * 1.1).toInt(), maxBackoffDelta)
                        communicationManager.sendPacket(GetBlockAtHeight(height), it.first())
                        parallelRequestsState[height] = timer.copy(backoffDelta = backoffDelta, lastSentTimestamp = Date().time)
                    }
                }
    }
}
