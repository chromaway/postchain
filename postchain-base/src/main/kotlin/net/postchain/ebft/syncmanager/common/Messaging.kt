package net.postchain.ebft.syncmanager.common

import mu.KLogging
import net.postchain.core.BlockQueries
import net.postchain.ebft.message.*
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID

abstract class Messaging(val blockQueries: BlockQueries, val communicationManager: CommunicationManager<Message>) {
    companion object: KLogging()

    /**
     * Send message to node including the block at [height]. This is a response to the [GetBlockAtHeight] request.
     *
     * @param peerId XPeerID of receiving node
     * @param height requested block height
     */
    fun sendBlockAtHeight(peerId: XPeerID, height: Long) {
        val blockData = blockQueries.getBlockAtHeight(height)
        blockData success {
            val packet = CompleteBlock(BlockData(it.header.rawData, it.transactions), height, it.witness.getRawData())
            communicationManager.sendPacket(packet, peerId)
        } fail {
            logger.error("No block at height $height, as requested by $peerId", it)
        }
    }

    fun sendBlockHeaderAndBlock(peerID: XPeerID, height: Long, myHeight: Long) {
        logger.debug("GetBlockHeaderAndBlock from peer $peerID for height $height, myHeight is $myHeight")
        val blockData = blockQueries.getBlockAtHeight(height)
        blockData success {
            val header = BlockHeader(it.header.rawData, it.witness.getRawData(), height)
            logger.debug("Replying with BlockHeader to peer $peerID for height $height")
            communicationManager.sendPacket(header, peerID)
            val unfinishedBlock = UnfinishedBlock(it.header.rawData, it.transactions)
            logger.debug("Replying with UnfinishedBlock to peer $peerID for height $height")
            communicationManager.sendPacket(unfinishedBlock, peerID)
        } fail {
            if (myHeight >= 0) {
                val block = blockQueries.getBlockAtHeight(myHeight, false).get()
                val header = BlockHeader(block.header.rawData, block.witness.getRawData(), height)
                logger.debug("Drained. Replying with BlockHeader to peer $peerID for height $myHeight")
                communicationManager.sendPacket(header, peerID)
            } else {
                // Send empty block header to signal that we have no blocks.
                val header = BlockHeader(byteArrayOf(), byteArrayOf(), height)
                logger.debug("Drained (no blocks). Replying with empty BlockHeader to peer $peerID")
                communicationManager.sendPacket(header, peerID)
            }
        }
    }
}