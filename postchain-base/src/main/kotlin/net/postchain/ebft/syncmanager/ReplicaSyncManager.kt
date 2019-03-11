package net.postchain.ebft.syncmanager

import mu.KLogging
import net.postchain.common.toHex
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

class ReplicaSyncManager(
        signers: List<ByteArray>,
        private val communicationManager: CommunicationManager<EbftMessage>,
        private val blockDatabase: BlockDatabase,
        val blockQueries: BlockQueries,
        val blockchainConfiguration: BlockchainConfiguration
) : SyncManagerBase {

    companion object : KLogging()

    enum class ReplicaState {
        ReceivingBlocks,  // fetch blocks from Validator nodes
        ValidateBlocks,   // verify and save block
    }

    private val nodePoolCount = 2 // used to select 1 random node
    private val waitMs = 500L

    private val validatorNodes: List<XPeerID> = signers.map { XPeerID(it) }

    private var replicaState = ReplicaState.ReceivingBlocks
    private var blockHeight: Long = blockQueries.getBestHeight().get()

    private var nodesWithWantedBlock = mutableSetOf<XPeerID>()

    private fun commitBlockAndResetState(block: BlockDataWithWitness) {
        blockDatabase.addBlock(block)
                .run {
                    always {
                        nodesWithWantedBlock.clear()
                        replicaState = ReplicaState.ReceivingBlocks
                    }
                    success {
                        blockHeight += 1
                    }
                    fail {
                        logger.error("[Replica] unable to add block: ${it.message}")
                        it.printStackTrace()
                    }
                }
    }

    override fun update() {
        logger.debug("==== Current block height: $blockHeight | state: $replicaState === ")
        dispatchMessages()
        Thread.sleep(waitMs)
    }

    private fun dispatchMessages() {
        for (packet in communicationManager.getPackets()) {
            val xPeerId = packet.first
            val message = packet.second
            try {
                when (message) {
                    is CompleteBlock -> {
                        logger.debug("[Replica] Complete block: ${xPeerId.byteArray.toHex()}")
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
                    else -> throw ProgrammerMistake("[Replica] Unhandled type ${message::class}")
                }
            } catch (e: Exception) {
                logger.error("[Replica] Couldn't handle message $message. Ignoring and continuing", e)
            }


            if(nodesWithWantedBlock.size >= nodePoolCount && replicaState == ReplicaState.ReceivingBlocks) {
                replicaState = ReplicaState.ValidateBlocks
                nodesWithWantedBlock
                        .toMutableList()
                        .also {
                            it.shuffle()
                            communicationManager.sendPacket(GetBlockAtHeight(blockHeight + 1), it.first())
                        }
            }
        }
    }
}
