package net.postchain.ebft.syncmanager.common

import mu.KLogging
import net.postchain.core.BlockQueries
import net.postchain.ebft.message.*
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID

abstract class Messaging(val blockQueries: BlockQueries, val communicationManager: CommunicationManager<Message>) {
    companion object: KLogging()

    /**
     * We're going to get a lot of requests from peers in fastsync mode. We should cache our tip
     * to be able to answer quicker and not bother the database with repeated queries.
     *
     * At least the header needs caching because we're sending it very often as a "I'm drained" signal,
     * but the full block might also be a good idea. Let's start with caching the header and see
     * if we need to cache the full block too.
     *
     * This cache sped up the SyncTest with 8 peers and 50 blocks (see SyncTest) from
     * about 40-56 seconds to about 34-47 seconds.
     */
    private var tipHeight: Long = -1
    private var tipHeader: BlockHeader = BlockHeader(byteArrayOf(), byteArrayOf(), 0)

    /**
     * Send message to node including the block at [height]. This is a response to the [GetBlockAtHeight] request.
     *
     * @param peerId XPeerID of receiving node
     * @param height requested block height
     */
    fun sendBlockAtHeight(peerId: XPeerID, height: Long) {
        val blockData = blockQueries.getBlockAtHeight(height).get()
        if (blockData == null) {
            logger.error("No block at height $height, as requested by $peerId")
            return
        }
        val packet = CompleteBlock(BlockData(blockData.header.rawData, blockData.transactions), height, blockData.witness.getRawData())
        communicationManager.sendPacket(packet, peerId)
    }

    fun sendBlockHeaderAndBlock(peerID: XPeerID, requestedHeight: Long, myHeight: Long) {
        logger.trace("GetBlockHeaderAndBlock from peer $peerID for height $requestedHeight, myHeight is $myHeight")

        if (myHeight == -1L) {
            sendHeader(peerID, byteArrayOf(), byteArrayOf(), -1, requestedHeight)
            return
        }

        if (myHeight < requestedHeight) {
            if (tipHeight == myHeight) {
                // We have a cached header. Make a copy with the correct requested height and send the copy.
                sendHeader(peerID, tipHeader.header, tipHeader.witness, tipHeight, requestedHeight)
                return
            }
            val block = blockQueries.getBlockAtHeight(myHeight, false).get()!!
            val h = sendHeader(peerID, block.header.rawData, block.witness.getRawData(), myHeight, requestedHeight)
            tipHeader = h
            tipHeight = myHeight
            return
        }

        val blockData = blockQueries.getBlockAtHeight(requestedHeight).get()!!
        val header = sendHeader(peerID, blockData.header.rawData, blockData.witness.getRawData(), requestedHeight, requestedHeight)
        if (requestedHeight == myHeight) {
            tipHeight = myHeight
            tipHeader = header
        }

        val unfinishedBlock = UnfinishedBlock(blockData.header.rawData, blockData.transactions)
        logger.trace("Replying with UnfinishedBlock to peer $peerID for height $requestedHeight")
        communicationManager.sendPacket(unfinishedBlock, peerID)
    }

    private fun sendHeader(peerID: XPeerID, header: ByteArray, witness: ByteArray, sentHeight: Long, requestedHeight: Long): BlockHeader {
        val h = BlockHeader(header, witness, requestedHeight)
        logger.trace("Replying with BlockHeader at height $sentHeight to peer $peerID for requested height $requestedHeight")
        communicationManager.sendPacket(h, peerID)
        return h
    }
}