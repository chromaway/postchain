// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.syncmanager.common

import mu.KLogging
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
import nl.komponents.kovenant.Promise
import java.lang.Integer.min
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
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

    /**
     * This is used to keep track of all blocks that are currently committing, on enqued
     * to be committed. When we leave fast sync mode by calling reset(), we'll be able to know what
     * work is currently ongoing, so that we can await that work to finish before returning from
     * reset.
     */
    private val committingBlocks = ConcurrentLinkedQueue<Promise<Unit, Exception>>( )

    val blockHeightAheadCount = 3

    companion object: KLogging()

    var blockHeight: Long = blockQueries.getBestHeight().get()
        private set

    fun sync() {
        blockHeight = blockQueries.getBestHeight().get()
        checkBlock()
        processState()
        dispatchMessages()
    }

    fun reset() {
        parallelRequestsState.clear()
        blocks.clear()
        // Await addBlock calls to be fulfilled to not interfere with normal sync.
        // If we don't wait, blocks asynchronously added by this class might get
        // committed AFTER normal sync is fastforwarded.
        while (true) {
            val p = committingBlocks.peek() ?: return
            try {
                p.get()
            } catch (e: Exception) {
                // Ignore here. It's handled in commitBlock()
            }
        }
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
     * Send message to node including the block at [height]. This is a response to the [GetBlockAtHeight] request.
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
        val p = blockDatabase.addBlock(block)
        committingBlocks.add(p)
        p.success {_ ->
            committingBlocks.remove(p)
            fastSyncAlgorithmTelemetry.blockAppendedToDatabase(blockHeight)
            parallelRequestsState.remove(blockHeight)
            blockHeight += 1
            // Uncomment for now until a better, more robust approach is implemented.
            // This means that we'll only commit a single block for each invocation
            // of sync()
//            checkBlock()
        }
        p.fail {
            parallelRequestsState.remove(blockHeight)
            committingBlocks.remove(p)
            blockHeight = blockQueries.getBestHeight().get()
            fastSyncAlgorithmTelemetry.failedToAppendBlockToDatabase(blockHeight, it.message)
            logger.debug("Error adding Block: ", it)
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
