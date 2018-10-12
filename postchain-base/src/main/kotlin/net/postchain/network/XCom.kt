package net.postchain.network

import mu.KLogging
import net.postchain.base.PeerCommConfiguration
import net.postchain.core.NODE_ID_READ_ONLY
import net.postchain.core.byteArrayKeyOf

/* A L I E N S */

class XCom<PacketType>(
        val xConn: XConnectionManager,
        val config: PeerCommConfiguration,
        val chainID: Long,
        val packetConverter: PacketConverter<PacketType>
        )
    : CommunicationManager<PacketType> {

    companion object : KLogging()

    var inboundPackets = mutableListOf<Pair<Int, PacketType>>()

    init {
        val peerconf = XChainPeerConfiguration(chainID,
                config,
                { data: ByteArray, peerID: XPeerID ->
                    decodeAndEnqueue(peerID, data)
                },
                packetConverter)

        xConn.connectChain(peerconf, true)
    }

    override fun broadcastPacket(packet: PacketType) {
        xConn.broadcastPacket(
                { packetConverter.encodePacket(packet) },
                chainID
        )
    }

    override fun sendPacket(packet: PacketType, recipients: Set<Int>) {
        assert(recipients.size == 1)
        val peerIdx = recipients.take(0)[0]
        xConn.sendPacket(
                { packetConverter.encodePacket(packet) },
                chainID, config.peerInfo[peerIdx].pubKey.byteArrayKeyOf()
        )
    }


    @Synchronized
    override fun getPackets(): MutableList<Pair<Int, PacketType>> {
        val currentQueue = inboundPackets
        inboundPackets = mutableListOf<Pair<Int, PacketType>>()
        return currentQueue
    }

    private fun getPeerIndex(peerID: XPeerID): Int {
        for (pi in config.peerInfo.withIndex()) {
            if (pi.value.pubKey.contentEquals(peerID.byteArray)) {
                return pi.index
            }
        }
        return NODE_ID_READ_ONLY
    }

    private fun decodeAndEnqueue(peerID: XPeerID, packet: ByteArray) {
        // packet decoding should not be synchronized so we can make
        // use of parallel processing in different threads
        val decodedPacket = packetConverter.decodePacket(
                peerID.byteArray, packet)
        synchronized(this) {
            inboundPackets.add(Pair(
                    getPeerIndex(peerID),
                    decodedPacket))
        }
    }

}