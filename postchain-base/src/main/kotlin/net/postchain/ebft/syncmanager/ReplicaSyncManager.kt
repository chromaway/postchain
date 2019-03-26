package net.postchain.ebft.syncmanager

import mu.KLogging
import net.postchain.core.BlockDataWithWitness
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.ProgrammerMistake
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.message.CompleteBlock
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.message.GetBlockAtHeight
import net.postchain.ebft.message.Status
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID
import java.lang.Integer.min
import java.util.*

class ReplicaSyncManager(
        signers: List<ByteArray>,
        private val communicationManager: CommunicationManager<EbftMessage>,
        private val blockDatabase: BlockDatabase,
        val blockQueries: BlockQueries,
        val blockchainConfiguration: BlockchainConfiguration
) : SyncManagerBase {

    companion object : KLogging()

    internal data class IssuedRequestTimer(val backoffDelta: Int, val lastSentTimestamp: Long)

    internal data class IncomingBlock(val block: BlockDataWithWitness, val height: Long): Comparable<IncomingBlock> {
        override fun compareTo(other: IncomingBlock) = when {
            height < other.height -> -1
            height > other.height -> 1
            else -> 0
        }
    }

    private val nodePoolCount = 2 // used to select 1 random node
    private val defaultBackoffDelta = 1000
    private val maxBackoffDelta = 30 * defaultBackoffDelta
    private val validatorNodes: List<XPeerID> = signers.map { XPeerID(it) }

    private var blockHeight: Long = blockQueries.getBestHeight().get()
    private var nodesWithBlocks = hashMapOf<XPeerID, Long>()

    private val parallelism = 10
    private var parallelRequestsState = hashMapOf<Long, IssuedRequestTimer>()

    private var blocks = PriorityQueue<IncomingBlock>(parallelism)

    private fun commitBlock(block: BlockDataWithWitness) {
        blockDatabase.addBlock(block)
                .run {
                    success {
                        parallelRequestsState.remove(blockHeight)
                        blockHeight += 1
                        checkBlock()
                    }
                    fail {
                        logger.error("Failed to add block: ${it.message}")
                        it.printStackTrace()
                    }
                }
    }

    override fun update() {
        logger.debug("STATUS: blockCount: ${blocks.count()} | parallelRequestsState count: ${parallelRequestsState.count()} ")
        checkBlock()
        processState()
        dispatchMessages()
    }

    private fun checkBlock() {
        if(blocks.peek() != null) {
            when(blocks.peek().height) {
                blockHeight -> blocks.remove()
                blockHeight + 1 -> commitBlock(blocks.remove().block)
            }
        }
    }

    private fun processState() {
        val maxElem = parallelRequestsState.maxBy { it.key }?.key ?: blockHeight
        val diff = parallelism - parallelRequestsState.count()
        (maxElem + 1 until maxElem + diff + 1).subtract(parallelRequestsState.keys).map { askForBlock(it) }

        parallelRequestsState.entries.associate {
            val state = IssuedRequestTimer(it.value.backoffDelta, it.value.lastSentTimestamp)
            if(!doesQueueContainsBlock(it.key) && Date().time > state.lastSentTimestamp + state.backoffDelta){
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
                if(it.count() >= nodePoolCount) {
                    logger.debug("Ask for block: $height | current height: $blockHeight")
                    val timer = parallelRequestsState[height]?: IssuedRequestTimer(defaultBackoffDelta, Date().time)
                    val backoffDelta = min((timer.backoffDelta.toDouble() * 1.1).toInt(), maxBackoffDelta)
                    communicationManager.sendPacket(GetBlockAtHeight(height), it.first())
                    parallelRequestsState[height] = timer.copy(backoffDelta = backoffDelta, lastSentTimestamp = Date().time)
                }
            }
    }

    private fun doesQueueContainsBlock(height: Long) = blocks.firstOrNull{ it.height == height} != null

    private fun dispatchMessages() {
        for (packet in communicationManager.getPackets()) {
            val xPeerId = packet.first
            val message = packet.second
            try {
                when (message) {
                    is CompleteBlock -> {
                        if(!doesQueueContainsBlock(message.height) && message.height > blockHeight) {
                            blocks.offer(IncomingBlock(decodeBlockDataWithWitness(message, blockchainConfiguration), message.height))
                        }
                    }
                    is Status -> {
                        if(xPeerId in validatorNodes){
                            nodesWithBlocks[xPeerId] = message.height - 1
                        }
                    }
                    else -> throw ProgrammerMistake("Unhandled type ${message::class}")
                }
            } catch (e: Exception) {
                logger.error("Couldn't handle message $message. Ignoring and continuing", e)
            }
        }
    }
}
