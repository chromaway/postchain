package net.postchain.ebft.syncmanager

import mu.KLogging
import net.postchain.core.BlockDataWithWitness
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.ProgrammerMistake
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.NodeStatus
import net.postchain.ebft.message.CompleteBlock
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.message.GetBlockAtHeight
import net.postchain.ebft.message.Status
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID
import java.util.Date

class ReplicaSyncManager(
        signers: List<ByteArray>,
        private val communicationManager: CommunicationManager<EbftMessage>,
        private val blockDatabase: BlockDatabase,
        val blockQueries: BlockQueries,
        val blockchainConfiguration: BlockchainConfiguration
) : SyncManagerBase {

    companion object : KLogging()

    enum class ReplicaState {
        Waiting,          // wait for validator nodes that has wanted block
        ReceivingBlocks,  // fetch blocks from Validator nodes
        ValidateBlocks,   // verify and save block
    }

    private val nodePoolCount = 2 // used to select 1 random node
    private val defaultBackoffDelta = 1000
    private val maxBackoffDelta = 30 * defaultBackoffDelta
    private var backoffDelta = defaultBackoffDelta
    private var lastSentTimestamp: Long = Date().time

    private val validatorNodes: List<XPeerID> = signers.map { XPeerID(it) }

    private var replicaState = ReplicaState.Waiting
    private var blockHeight: Long = blockQueries.getBestHeight().get()

    private var nodesWithWantedBlock = mutableSetOf<XPeerID>()

    private fun commitBlockAndResetState(block: BlockDataWithWitness) {
        blockDatabase.addBlock(block)
                .run {
                    always {
                        backoffDelta = defaultBackoffDelta
                        nodesWithWantedBlock.clear()
                        replicaState = ReplicaState.ReceivingBlocks
                    }
                    success {
                        blockHeight += 1
                    }
                    fail {
                        logger.error("Failed to add block: ${it.message}")
                        it.printStackTrace()
                    }
                }
    }

    override fun update() {
        if(System.currentTimeMillis() % 30 == 0L){
            logger.debug("block height: $blockHeight")
        }
        processState()
        dispatchMessages()
    }

    private fun processState() {
        if(nodesWithWantedBlock.size < nodePoolCount) {
            replicaState = ReplicaState.Waiting
        } else {
            if(replicaState != ReplicaState.ValidateBlocks) {
                askForBlock()
            } else {
                // retry if we don't have response lastSentTimestamp + backoffDelta
                if(Date().time > lastSentTimestamp + backoffDelta){
                    if(backoffDelta < maxBackoffDelta){
                        backoffDelta = (backoffDelta.toDouble() * 1.1).toInt()
                    }
                    askForBlock()
                }
            }
        }
    }

    private fun askForBlock() {
        replicaState = ReplicaState.ReceivingBlocks
        nodesWithWantedBlock
            .toMutableList()
            .also {
                it.shuffle()
                communicationManager.sendPacket(GetBlockAtHeight(blockHeight + 1), it.first())
                lastSentTimestamp = Date().time
                replicaState = ReplicaState.ValidateBlocks
            }
    }

    private fun dispatchMessages() {
        for (packet in communicationManager.getPackets()) {
            val xPeerId = packet.first
            val message = packet.second
            try {
                when (message) {
                    is CompleteBlock -> {
                        val blockDataWithWitness = decodeBlockDataWithWitness(message, blockchainConfiguration)
                        if (message.height == blockHeight + 1) {
                            commitBlockAndResetState(blockDataWithWitness)
                        }
                    }
                    is Status -> {
                        val nodeStatus = NodeStatus(message.height, message.serial)
                        val nodeBlockHeight = nodeStatus.height - 1
                        if(nodeBlockHeight > blockHeight && xPeerId in validatorNodes){
                            nodesWithWantedBlock.add(xPeerId)
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
