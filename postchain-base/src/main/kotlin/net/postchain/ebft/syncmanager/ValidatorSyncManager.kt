// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.ebft.syncmanager

import mu.KLogging
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.core.Signature
import net.postchain.ebft.*
import net.postchain.ebft.message.*
import net.postchain.ebft.message.BlockData
import net.postchain.ebft.message.Transaction
import net.postchain.ebft.rest.contract.serialize
import net.postchain.ebft.syncmanager.replica.FastSyncAlgorithm
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID
import java.util.*

fun decodeBlockDataWithWitness(block: CompleteBlock, bc: BlockchainConfiguration)
        : BlockDataWithWitness {
    val header = bc.decodeBlockHeader(block.data.header)
    val witness = bc.decodeWitness(block.witness)
    return BlockDataWithWitness(header, block.data.transactions, witness)
}

fun decodeBlockData(block: BlockData, bc: BlockchainConfiguration)
        : net.postchain.core.BlockData {
    val header = bc.decodeBlockHeader(block.header)
    return net.postchain.core.BlockData(header, block.transactions)
}

private class StatusSender(
        private val maxStatusInterval: Int,
        private val statusManager: StatusManager,
        private val communicationManager: CommunicationManager<Message>
) {
    var lastSerial: Long = -1
    var lastSentTime: Long = Date(0L).time

    // Sends a status message to all peers when my status has changed or
    // after a timeout period.
    fun update() {
        val myStatus = statusManager.myStatus
        val isNewState = myStatus.serial > this.lastSerial
        val timeoutExpired = System.currentTimeMillis() - this.lastSentTime > this.maxStatusInterval
        if (isNewState || timeoutExpired) {
            this.lastSentTime = Date().time
            this.lastSerial = myStatus.serial
            val statusMessage = Status(myStatus.blockRID, myStatus.height,
                    myStatus.revolting, myStatus.round, myStatus.serial,
                    myStatus.state.ordinal)
            communicationManager.broadcastPacket(statusMessage)
        }
    }
}

/**
 * The ValidatorSyncManager handles communications with our peers.
 */
class ValidatorSyncManager(
        private val blockchainProcessName: String,
        private val signers: List<ByteArray>,
        private val statusManager: StatusManager,
        private val blockManager: BlockManager,
        private val blockDatabase: BlockDatabase,
        private val blockQueries: BlockQueries,
        private val communicationManager: CommunicationManager<Message>,
        private val nodeStateTracker: NodeStateTracker,
        private val txQueue: TransactionQueue,
        private val blockchainConfiguration: BlockchainConfiguration
) : SyncManagerBase {
    private val revoltTracker = RevoltTracker(10000, statusManager)
    private val statusSender = StatusSender(1000, statusManager, communicationManager)
    private val defaultTimeout = 1000
    private var currentTimeout: Int
    private var processingIntent: BlockIntent
    private var processingIntentDeadline = 0L
    private var lastStatusLogged: Long

    @Volatile
    private var useFastSyncAlgorithm: Boolean = false
    private val fastSyncAlgorithm = FastSyncAlgorithm(
            blockchainProcessName,
            signers.minus(signers.elementAt((statusManager as BaseStatusManager).myIndex)),
            communicationManager,
            blockDatabase,
            blockchainConfiguration,
            blockQueries
    )

    companion object : KLogging()

    //    private val nodes = communicationManager.peers().map { XPeerID(it.pubKey) }
    private val signersIds = signers.map { XPeerID(it) }

    private fun indexOfValidator(peerId: XPeerID): Int = signersIds.indexOf(peerId)
    //        return nodes.indexOf(peerID)

    private fun validatorAtIndex(index: Int): XPeerID = signersIds[index]

    /**
     * Handle incoming messages
     */
    private fun dispatchMessages() {
        for (packet in communicationManager.getPackets()) {
            val (xPeerId, message) = packet

            val nodeIndex = indexOfValidator(xPeerId)
            if (nodeIndex == -1) continue

            logger.trace { "[$blockchainProcessName]: Received message type ${message.javaClass.simpleName} from node $nodeIndex" }

            try {
                when (message) {
                    // same case for replica and validator node
                    is GetBlockAtHeight -> sendBlockAtHeight(xPeerId, message.height)
                    else -> {
                        if (nodeIndex != NODE_ID_READ_ONLY) {
                            // validator consensus logic
                            when (message) {
                                is Status -> {
                                    val nodeStatus = NodeStatus(message.height, message.serial)
                                    useFastSyncAlgorithm = (message.height - statusManager.myStatus.height) >= fastSyncAlgorithm.blockHeightAheadCount
                                    nodeStatus.blockRID = message.blockRID
                                    nodeStatus.revolting = message.revolting
                                    nodeStatus.round = message.round
                                    nodeStatus.state = NodeState.values()[message.state]
                                    statusManager.onStatusUpdate(nodeIndex, nodeStatus)
                                }
                                is BlockSignature -> {
                                    val signature = Signature(message.sig.subjectID, message.sig.data)
                                    val smBlockRID = this.statusManager.myStatus.blockRID
                                    if (smBlockRID == null) {
                                        logger.info("[$blockchainProcessName]: Received signature not needed")
                                    } else if (!smBlockRID.contentEquals(message.blockRID)) {
                                        logger.info("[$blockchainProcessName]: Receive signature for a different block")
                                    } else if (this.blockDatabase.verifyBlockSignature(signature)) {
                                        this.statusManager.onCommitSignature(nodeIndex, message.blockRID, signature)
                                    }
                                }
                                is CompleteBlock -> {
                                    try {
                                        blockManager.onReceivedBlockAtHeight(
                                                decodeBlockDataWithWitness(message, blockchainConfiguration),
                                                message.height)
                                    } catch (e: Exception) {
                                        logger.error("Failed to add block to database. Resetting state...", e)
                                        // reset state to last known from database
                                        val currentBlockHeight = blockQueries.getBestHeight().get()
                                        statusManager.fastForwardHeight(currentBlockHeight + 1)
                                    }
                                }
                                is UnfinishedBlock -> {
                                    blockManager.onReceivedUnfinishedBlock(decodeBlockData(BlockData(message.header, message.transactions),
                                            blockchainConfiguration))
                                }
                                is GetUnfinishedBlock -> sendUnfinishedBlock(nodeIndex)
                                is GetBlockSignature -> sendBlockSignature(nodeIndex, message.blockRID)
                                is Transaction -> handleTransaction(nodeIndex, message)
                                else -> throw ProgrammerMistake("Unhandled type ${message::class}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("[$blockchainProcessName]: Couldn't handle message $message. Ignoring and continuing", e)
            }
        }
    }

    /**
     * Handle transaction received from peer
     *
     * @param index
     * @param message message including the transaction
     */
    private fun handleTransaction(index: Int, message: Transaction) {
        val tx = blockchainConfiguration.getTransactionFactory().decodeTransaction(message.data)
        if (!tx.isCorrect()) {
            throw UserMistake("Transaction ${tx.getRID()} is not correct")
        }
        txQueue.enqueue(tx)
    }

    /**
     * Send message to peer with our commit signature
     *
     * @param nodeIndex node index of receiving peer
     * @param blockRID block identifier
     */
    private fun sendBlockSignature(nodeIndex: Int, blockRID: ByteArray) {
        val currentBlock = this.blockManager.currentBlock
        if (currentBlock != null && currentBlock.header.blockRID.contentEquals(blockRID)) {
            if (!statusManager.myStatus.blockRID!!.contentEquals(currentBlock.header.blockRID)) {
                throw ProgrammerMistake("status manager block RID (${statusManager.myStatus.blockRID!!.toHex()}) out of sync with current block RID (${currentBlock.header.blockRID.toHex()})")
            }
            val signature = statusManager.getCommitSignature()
            if (signature != null) {
                communicationManager.sendPacket(BlockSignature(
                        blockRID,
                        net.postchain.ebft.message.Signature(signature.subjectID, signature.data)),
                        validatorAtIndex(nodeIndex))
            }
            return
        }
        val blockSignature = blockDatabase.getBlockSignature(blockRID)
        blockSignature success {
            val packet = BlockSignature(blockRID, net.postchain.ebft.message.Signature(it.subjectID, it.data))
            communicationManager.sendPacket(packet, validatorAtIndex(nodeIndex))
        } fail {
            logger.debug("[$blockchainProcessName]: Error sending BlockSignature", it)
        }
    }

    /**
     * Send message to node including the block at [height]. This is a response to the [fetchBlockAtHeight] request.
     *
     * @param xPeerId XPeerID of receiving node
     * @param height requested block height
     */
    private fun sendBlockAtHeight(xPeerId: XPeerID, height: Long) {
        val blockAtHeight = blockDatabase.getBlockAtHeight(height)
        blockAtHeight success {
            val packet = CompleteBlock(
                    BlockData(it.header.rawData, it.transactions),
                    height,
                    it.witness!!.getRawData()
            )
            communicationManager.sendPacket(packet, xPeerId)
        } fail { logger.debug("[$blockchainProcessName]: Error sending CompleteBlock", it) }
    }

    /**
     * Send message to node with the current unfinished block.
     *
     * @param nodeIndex index of node to send block to
     */
    private fun sendUnfinishedBlock(nodeIndex: Int) {
        val currentBlock = blockManager.currentBlock
        if (currentBlock != null) {
            communicationManager.sendPacket(UnfinishedBlock(currentBlock.header.rawData, currentBlock.transactions.toList()),
                    validatorAtIndex(nodeIndex))
        }
    }

    /**
     * Pick a random node from all nodes matching certain conditions
     *
     * @param match function that checks whether a node matches our selection conditions
     * @return index of selected node
     */
    private fun selectRandomNode(match: (NodeStatus) -> Boolean): Int? {
        val matchingIndexes = mutableListOf<Int>()
        statusManager.nodeStatuses.forEachIndexed { index, status ->
            if (match(status)) matchingIndexes.add(index)
        }
        if (matchingIndexes.isEmpty()) return null
        if (matchingIndexes.size == 1) return matchingIndexes[0]
        return matchingIndexes[Math.floor(Math.random() * matchingIndexes.size).toInt()]
    }

    /**
     * Send message to random peer to retrieve the block at [height]
     *
     * @param height the height at which we want the block
     */
    private fun fetchBlockAtHeight(height: Long) {
        val nodeIndex = selectRandomNode { it.height > height } ?: return
        logger.debug("[$blockchainProcessName]: Fetching block at height $height from node $nodeIndex")
        communicationManager.sendPacket(GetBlockAtHeight(height), validatorAtIndex(nodeIndex))
    }

    /**
     * Send message to fetch commit signatures from [nodes]
     *
     * @param blockRID identifier of the block to fetch signatures for
     * @param nodes list of nodes we want commit signatures from
     */
    private fun fetchCommitSignatures(blockRID: ByteArray, nodes: Array<Int>) {
        val message = GetBlockSignature(blockRID)
        logger.debug("[$blockchainProcessName]: Fetching commit signature for block with RID ${blockRID.toHex()} from nodes ${Arrays.toString(nodes)}")
        nodes.forEach {
            communicationManager.sendPacket(message, validatorAtIndex(it))
        }
    }

    /**
     * Send message to random peer for fetching latest unfinished block at the same height as us
     *
     * @param blockRID identifier of the unfinished block
     */
    private fun fetchUnfinishedBlock(blockRID: ByteArray) {
        val height = statusManager.myStatus.height
        val nodeIndex = selectRandomNode {
            it.height == height && (it.blockRID?.contentEquals(blockRID) ?: false)
        } ?: return
        logger.debug("[$blockchainProcessName]: Fetching unfinished block with RID ${blockRID.toHex()} from node $nodeIndex ")
        communicationManager.sendPacket(GetUnfinishedBlock(blockRID), validatorAtIndex(nodeIndex))
    }

    /**
     * Process our intent latest intent
     */
    fun processIntent() {
        val intent = blockManager.getBlockIntent()
        if (intent == processingIntent) {
            if (intent is DoNothingIntent) return
            if (Date().time > processingIntentDeadline) {
                this.currentTimeout = (this.currentTimeout.toDouble() * 1.1).toInt() // exponential back-off
            } else {
                return
            }
        } else {
            currentTimeout = defaultTimeout
        }
        when (intent) {
            DoNothingIntent -> Unit
            is FetchBlockAtHeightIntent -> if(!useFastSyncAlgorithm) { fetchBlockAtHeight(intent.height) }
            is FetchCommitSignatureIntent -> fetchCommitSignatures(intent.blockRID, intent.nodes)
            is FetchUnfinishedBlockIntent -> fetchUnfinishedBlock(intent.blockRID)
            else -> throw ProgrammerMistake("Unrecognized intent: ${intent::class}")
        }
        processingIntent = intent
        processingIntentDeadline = Date().time + currentTimeout
    }

    /**
     * Log status of all nodes including their latest block RID and if they have the signature or not
     */
    private fun logStatus() {
        for ((idx, ns) in statusManager.nodeStatuses.withIndex()) {
            val blockRID = ns.blockRID
            val haveSignature = statusManager.commitSignatures[idx] != null
            logger.info {
                "[$blockchainProcessName]: node:$idx he:${ns.height} ro:${ns.round} st:${ns.state}" +
                        (if (ns.revolting) " R" else "") +
                        " blockRID:${blockRID?.toHex() ?: "null"}" +
                        " havesig:$haveSignature"
            }
        }
    }

    /**
     * Process peer messages, how we should proceed with the current block, updating the revolt tracker and
     * notify peers of our current status.
     */
    override fun update() {
        if(useFastSyncAlgorithm) {
            fastSyncAlgorithm.sync()
            if(fastSyncAlgorithm.isUpToDate()){
                useFastSyncAlgorithm = false
                val currentBlockHeight = blockQueries.getBestHeight().get()
                statusManager.fastForwardHeight(currentBlockHeight + 1)
            }
        } else {

            // Process all messages from peers, one at a time. Some
            // messages may trigger asynchronous code which will
            // send replies at a later time, others will send replies
            // immediately
            dispatchMessages()

            // An intent is something that we want to do with our current block.
            // The current intent is fetched from the BlockManager and will result in
            // some messages being sent to peers requesting data like signatures or
            // complete blocks
            processIntent()

            // RevoltTracker will check trigger a revolt if conditions for revolting are met
            // A revolt will be triggerd by calling statusManager.onStartRevolting()
            // Typical revolt conditions
            //    * A timeout happens and round has not increased. Round is increased then 2f+1 nodes
            //      are revolting.
            revoltTracker.update()

            // Sends a status message to all peers when my status has changed or after a timeout
            statusSender.update()

            nodeStateTracker.myStatus = statusManager.myStatus.serialize()
            nodeStateTracker.nodeStatuses = statusManager.nodeStatuses.map { it.serialize() }.toTypedArray()
            nodeStateTracker.blockHeight = statusManager.myStatus.height

            if (Date().time - lastStatusLogged >= StatusLogInterval) {
                logStatus()
                lastStatusLogged = Date().time
            }
        }
    }

    init {
        this.currentTimeout = defaultTimeout
        this.processingIntent = DoNothingIntent
        this.lastStatusLogged = Date().time
    }
}