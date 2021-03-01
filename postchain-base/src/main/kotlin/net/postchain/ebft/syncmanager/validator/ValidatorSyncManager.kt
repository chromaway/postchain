// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.syncmanager.validator

import mu.KLogging
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.core.Signature
import net.postchain.ebft.*
import net.postchain.ebft.message.*
import net.postchain.ebft.message.BlockData
import net.postchain.ebft.message.Transaction
import net.postchain.ebft.rest.contract.serialize
import net.postchain.ebft.syncmanager.BlockDataDecoder.decodeBlockData
import net.postchain.ebft.syncmanager.BlockDataDecoder.decodeBlockDataWithWitness
import net.postchain.ebft.syncmanager.StatusLogInterval
import net.postchain.ebft.syncmanager.SyncManager
import net.postchain.ebft.syncmanager.common.EBFTNodesCondition
import net.postchain.ebft.syncmanager.common.FastSyncParameters
import net.postchain.ebft.syncmanager.common.FastSynchronizer
import net.postchain.ebft.syncmanager.common.Messaging
import net.postchain.ebft.worker.WorkerContext
import net.postchain.network.x.XPeerID
import nl.komponents.kovenant.task
import java.util.*

/**
 * The ValidatorSyncManager handles communications with our peers.
 */
class ValidatorSyncManager(private val workerContext: WorkerContext,
                           private val statusManager: StatusManager,
                           private val blockManager: BlockManager,
                           private val blockDatabase: BlockDatabase,
                           private val nodeStateTracker: NodeStateTracker
) : Messaging(workerContext.engine.getBlockQueries(), workerContext.communicationManager), SyncManager {
    private val blockchainConfiguration = workerContext.engine.getConfiguration()
    private val revoltTracker = RevoltTracker(10000, statusManager)
    private val statusSender = StatusSender(1000, statusManager, workerContext.communicationManager)
    private val defaultTimeout = 1000
    private var currentTimeout: Int
    private var processingIntent: BlockIntent
    private var processingIntentDeadline = 0L
    private var lastStatusLogged: Long
    private val processName = workerContext.processName
    private var useFastSyncAlgorithm: Boolean = true
    companion object : KLogging()

    private val fastSynchronizer: FastSynchronizer

    init {
        this.currentTimeout = defaultTimeout
        this.processingIntent = DoNothingIntent
        this.lastStatusLogged = Date().time
        val params = FastSyncParameters()
        params.exitDelay = workerContext.nodeConfig.fastSyncExitDelay
        params.mustSyncUntilHeight = workerContext.nodeConfig.mustSyncUntilHeight?.get(blockchainConfiguration.chainID) ?: -1
        fastSynchronizer = FastSynchronizer(workerContext,
                blockDatabase,
                params
        )
    }

    //    private val nodes = communicationManager.peers().map { XPeerID(it.pubKey) }
    private val signersIds = workerContext.signers.map { XPeerID(it) }

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
            val isReadOnlyNode = nodeIndex == -1 // This must be a read-only node since not in the validator list

            logger.trace { "$processName: Received message type ${message.javaClass.simpleName} from node $nodeIndex" }

            try {
                when (message) {
                    // same case for replica and validator node
                    is GetBlockAtHeight -> sendBlockAtHeight(xPeerId, message.height)
                    is GetBlockHeaderAndBlock -> {
                        sendBlockHeaderAndBlock(xPeerId, message.height,
                                this.statusManager.myStatus.height-1)
                    }
                    else -> {
                        if (!isReadOnlyNode) { // TODO: [POS-90]: Is it necessary here `isReadOnlyNode`?
                            // validator consensus logic
                            when (message) {
                                is Status -> {
                                    NodeStatus(message.height, message.serial)
                                            .apply {
                                                blockRID = message.blockRID
                                                revolting = message.revolting
                                                round = message.round
                                                state = NodeState.values()[message.state]
                                            }.also {
                                                statusManager.onStatusUpdate(nodeIndex, it)
                                            }

                                    tryToSwitchToFastSync()
                                }
                                is BlockSignature -> {
                                    val signature = Signature(message.sig.subjectID, message.sig.data)
                                    val smBlockRID = this.statusManager.myStatus.blockRID
                                    if (smBlockRID == null) {
                                        logger.info("$processName: Received signature not needed")
                                    } else if (!smBlockRID.contentEquals(message.blockRID)) {
                                        logger.info("$processName: Receive signature for a different block")
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
                                        statusManager.fastForwardHeight(currentBlockHeight)
                                        blockManager.currentBlock = null
                                    }
                                }
                                is UnfinishedBlock -> {
                                    blockManager.onReceivedUnfinishedBlock(
                                            decodeBlockData(
                                                    BlockData(message.header, message.transactions),
                                                    blockchainConfiguration)
                                    )
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
                logger.error("$processName: Couldn't handle message $message. Ignoring and continuing", e)
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
        // TODO: reject if queue is full
        task {
            val tx = blockchainConfiguration.getTransactionFactory().decodeTransaction(message.data)
            workerContext.engine.getTransactionQueue().enqueue(tx)
        }

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
            logger.debug("$processName: Error sending BlockSignature", it)
        }
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
        logger.debug("$processName: Fetching block at height $height from node $nodeIndex")
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
        logger.debug("$processName: Fetching commit signature for block with RID ${blockRID.toHex()} from nodes ${Arrays.toString(nodes)}")
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
        logger.debug("$processName: Fetching unfinished block with RID ${blockRID.toHex()} from node $nodeIndex ")
        communicationManager.sendPacket(GetUnfinishedBlock(blockRID), validatorAtIndex(nodeIndex))
    }

    /**
     * Process our intent latest intent
     */
    private fun processIntent() {
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
            is FetchBlockAtHeightIntent -> if (!useFastSyncAlgorithm) {
                fetchBlockAtHeight(intent.height)
            }
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
        if (logger.isDebugEnabled) {
            val smIntent = statusManager.getBlockIntent()
            val bmIntent = blockManager.getBlockIntent()
            val primary = if (statusManager.isMyNodePrimary()) {
                "I'm primary, "
            } else {
                "(prim = ${statusManager.primaryIndex()}),"
            }
            logger.debug("$processName: My node: ${statusManager.getMyIndex()}, $primary block mngr: $bmIntent, status mngr: $smIntent")
        }
        for ((idx, ns) in statusManager.nodeStatuses.withIndex()) {
            val blockRID = ns.blockRID
            val haveSignature = statusManager.commitSignatures[idx] != null
            if (logger.isDebugEnabled) {
                logger.debug {
                    "$processName: node:$idx he:${ns.height} ro:${ns.round} st:${ns.state}" +
                            (if (ns.revolting) " R" else "") +
                            " blockRID:${blockRID?.toHex() ?: "null"}" +
                            " havesig:$haveSignature"
                }
            }
        }
    }

    private fun tryToSwitchToFastSync() {
        useFastSyncAlgorithm = EBFTNodesCondition(statusManager.nodeStatuses) { status ->
            status.height - statusManager.myStatus.height >= 3
        }.satisfied()
    }

    /**
     * Process peer messages, how we should proceed with the current block, updating the revolt tracker and
     * notify peers of our current status.
     */
    override fun update() {
        if (useFastSyncAlgorithm) {
            fastSynchronizer.syncUntilResponsiveNodesDrained()
            // turn off fast sync, reset current block to null, and query for the last known state from db to prevent
            // possible race conditions
            useFastSyncAlgorithm = false
            val currentBlockHeight = blockQueries.getBestHeight().get()
            statusManager.fastForwardHeight(currentBlockHeight)
            blockManager.currentBlock = null
        } else {
            synchronized(statusManager) {
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
    }

    fun isInFastSync(): Boolean {
        return useFastSyncAlgorithm
    }

    fun shutdown() {
        fastSynchronizer.shutdown()
    }
}