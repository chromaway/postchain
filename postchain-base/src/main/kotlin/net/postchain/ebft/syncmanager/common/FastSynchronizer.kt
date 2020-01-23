// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.syncmanager.common

import net.postchain.core.BlockDataWithWitness
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.ProgrammerMistake
import net.postchain.debug.BlockchainProcessName
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.NodeStatus
import net.postchain.ebft.message.*
import net.postchain.ebft.syncmanager.BlockDataDecoder.decodeBlockDataWithWitness
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID
import java.lang.Integer.min
import java.util.*
import kotlin.math.abs

class FastSynchronizer(
        processName: BlockchainProcessName,
        signers: List<ByteArray>,
        private val communicationManager: CommunicationManager<Message>,
        private val blockDatabase: BlockDatabase,
        private val blockchainConfiguration: BlockchainConfiguration,
        private val blockQueries: BlockQueries
) {
    private val fastSyncAlgorithmTelemetry = FastSynchronizerTelemetry(processName)
    private val validatorNodes: List<XPeerID> = signers.map { XPeerID(it) }
    private val parallelism = 10
    private val defaultBackoffDelta = 1000
    private val maxBackoffDelta = 30 * defaultBackoffDelta

    private var nodesStatuses = HashMap<Int, NodeStatus>()
    private var nodesWithBlocks = hashMapOf<XPeerID, Long>()
    private var parallelRequestsState = hashMapOf<Long, IssuedRequestTimer>()
    private var blocks = PriorityQueue<IncomingBlock>(parallelism)

    val blockHeightAheadCount = 3

    var blockHeight: Long = blockQueries.getBestHeight().get()
        private set

    fun sync() {
        checkBlock()
        processState()
        dispatchMessages()
    }

    fun reset() {
        parallelRequestsState.clear()
        blocks.clear()
    }

    fun isAlmostUpToDate(): Boolean {
        return blockHeight != -1L && EBFTNodesCondition(nodeStatuses()) { status ->
            abs(status.height - blockHeight) < blockHeightAheadCount
        }.satisfied()
    }

    fun nodeStatuses() = nodesStatuses.values.toTypedArray()

    fun logCurrentState() = fastSyncAlgorithmTelemetry.logCurrentState(blockHeight, parallelRequestsState, blocks)

    private fun checkBlock() {
        if (blocks.peek() != null) {
            blocks.peek().height.let {
                when {
                    it <= blockHeight -> blocks.remove()
                    it == blockHeight + 1 ->
                        commitBlock(blocks.remove().block)
                    else -> Unit
                }
            }
        }
    }

    private fun processState() {
        parallelRequestsState.entries.removeIf { it.key <= blockHeight }
        val maxElem = (parallelRequestsState.maxBy { it.key }?.key ?: blockHeight) + 1
        val diff = parallelism - parallelRequestsState.count()
        (maxElem until maxElem + diff).subtract(parallelRequestsState.keys).map {
            if (!doesQueueContainsBlock(it)) {
                askForBlock(it)
            }
        }

        parallelRequestsState.entries.associate {
            val state = IssuedRequestTimer(it.value.backoffDelta, it.value.lastSentTimestamp)
            if (!doesQueueContainsBlock(it.key) && Date().time > state.lastSentTimestamp + state.backoffDelta) {
                askForBlock(it.key)
            }
            it.key to state
        }
    }

    /**
     * Send message to node including the block at [height]. This is a response to the [fetchBlockAtHeight] request.
     *
     * @param peerId XPeerID of receiving node
     * @param height requested block height
     */
    private fun sendBlockAtHeight(peerId: XPeerID, height: Long) {
        val blockData = blockDatabase.getBlockAtHeight(height)
        blockData success {
            val packet = CompleteBlock(
                    BlockData(it.header.rawData, it.transactions),
                    height,
                    it.witness!!.getRawData()
            )
            communicationManager.sendPacket(packet, peerId)
        } fail { fastSyncAlgorithmTelemetry.fatal("Error sending CompleteBlock", it) }
    }

    private fun dispatchMessages() {
        for (packet in communicationManager.getPackets()) {
            val xPeerId = packet.first
            val message = packet.second
            try {
                when (message) {
                    is GetBlockAtHeight -> sendBlockAtHeight(xPeerId, message.height)
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
                        nodesStatuses[validatorNodes.indexOf(xPeerId)] = nodeStatus

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
        val aheadNodes = nodesWithBlocks
                .filter { it.value >= height }
                .map { it.key }
                .toMutableList()

        if (aheadNodes.isNotEmpty()) {
            aheadNodes.shuffle()
            fastSyncAlgorithmTelemetry.askForBlock(height, blockHeight)
            val timer = parallelRequestsState[height]
                    ?: IssuedRequestTimer(defaultBackoffDelta, Date().time)
            val backoffDelta = min((timer.backoffDelta.toDouble() * 1.1).toInt(), maxBackoffDelta)
            communicationManager.sendPacket(GetBlockAtHeight(height), aheadNodes.first())
            parallelRequestsState[height] = timer.copy(backoffDelta = backoffDelta, lastSentTimestamp = Date().time)
        }
    }

    private fun doesQueueContainsBlock(height: Long) = blocks.firstOrNull { it.height == height } != null
}
