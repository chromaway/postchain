package net.postchain.network

import mu.KLogging
import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.core.ByteArrayKey
import net.postchain.core.ProgrammerMistake

interface CommunicationManager<PacketType> {
    fun getPackets(): MutableList<Pair<Int, PacketType>>
    fun broadcastPacket(packet: PacketType)
    fun sendPacket(packet: PacketType, recipients: Set<Int>)
}

/**
 * Peer communication manager
 */
class CommManager<PacketType>(val config: PeerCommConfiguration,
                              private val connManager: PeerConnectionManager<PacketType>
) : CommunicationManager<PacketType>, BlockchainDataHandler {

    val myIndex: Int
    val blockchainRID: ByteArray
    val peers: Array<PeerInfo>
    val peerIDs: List<ByteArrayKey>
    var inboundPackets = mutableListOf<Pair<Int, PacketType>>()

    companion object : KLogging()

    init {
        myIndex = config.myIndex
        blockchainRID = config.blockchainRID
        peers = config.peerInfo
        peerIDs = peers.map { ByteArrayKey(it.pubKey) }
        connManager.registerBlockchain(blockchainRID, this)
        for ((index, peer) in peers.withIndex()) {
            if (index < myIndex) {
                connManager.connectPeer(peer, connManager.packetConverter) { decodeAndEnqueue(index, it) }
            }
        }
    }

    override fun getPacketHandler(peerPubKey: ByteArray): (ByteArray) -> Unit {
        val peerIndex = peers.indexOfFirst { it.pubKey.contentEquals(peerPubKey) }
        if (peerIndex > -1) {
            return { decodeAndEnqueue(peerIndex, it) }
        } else {
            TODO("Handle read-only peer?")
            //throw UserMistake("Got connection from unknown peer")
        }
    }

    @Synchronized
    override fun getPackets(): MutableList<Pair<Int, PacketType>> {
        val currentQueue = inboundPackets
        inboundPackets = mutableListOf<Pair<Int, PacketType>>()
        return currentQueue
    }

    override fun broadcastPacket(packet: PacketType) {
        connManager.sendPacket(OutboundPacket(packet, peerIDs))
    }

    override fun sendPacket(packet: PacketType, recipients: Set<Int>) {
        if (recipients.isEmpty()) {
            // Using recipients=emptySet() to broadcast may cause
            // code to accidentally broadcast, when in fact they want to send
            // the packet to exactly no recipients. So we don't allow that.
            throw ProgrammerMistake("Cannot send to no recipients. If you want to broadcast, please use broadcastPacket() instead")
        }
        logger.trace("Sending $myIndex -> $recipients: $packet")
        connManager.sendPacket(OutboundPacket(packet, recipients.map { peerIDs[it] }))
    }

    private fun decodeAndEnqueue(peerIndex: Int, packet: ByteArray) {
        // packet decoding should not be synchronized so we can make
        // use of parallel processing in different threads
        val decodedPacket = connManager.packetConverter.decodePacket(peers[peerIndex].pubKey, packet)
        logger.trace("Receiving $peerIndex -> $myIndex: $decodedPacket")
        synchronized(this) {
            inboundPackets.add(Pair(peerIndex, decodedPacket))
        }
    }
}