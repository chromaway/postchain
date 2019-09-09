package net.postchain.network.x

import mu.KLogging
import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.base.peerId
import net.postchain.devtools.PeerNameHelper.peerName
import net.postchain.network.CommunicationManager
import net.postchain.network.XPacketDecoder
import net.postchain.network.XPacketEncoder

class DefaultXCommunicationManager<PacketType>(
        val connectionManager: XConnectionManager,
        val config: PeerCommConfiguration,
        val chainID: Long,
        val blockchainRID: ByteArray,
        private val packetEncoder: XPacketEncoder<PacketType>,
        private val packetDecoder: XPacketDecoder<PacketType>,
        val processName: String = ""
) : CommunicationManager<PacketType> {

    companion object : KLogging()

    private var inboundPackets = mutableListOf<Pair<XPeerID, PacketType>>()

    override fun init() {
        val peerConfig = XChainPeerConfiguration(
                chainID,
                blockchainRID,
                config,
                { data: ByteArray, peerID: XPeerID -> decodeAndEnqueue(peerID, data) },
                packetEncoder,
                packetDecoder
        )

        connectionManager.connectChain(peerConfig, true)
    }

    override fun peers(): Array<PeerInfo> = config.peerInfo

    @Synchronized
    override fun getPackets(): MutableList<Pair<XPeerID, PacketType>> {
        val currentQueue = inboundPackets
        inboundPackets = mutableListOf()
        return currentQueue
    }

    override fun sendPacket(packet: PacketType, recipient: XPeerID) {
        logger.trace { "[$processName]: sendPacket($packet, ${peerName(recipient.toString())})" }

        val peers: List<XPeerID> = config.peerInfo.map(PeerInfo::peerId)
        require(recipient in peers) {
            "CommunicationManager.sendPacket(): recipient not found among peers"
        }

        require(XPeerID(config.pubKey) != recipient) {
            "CommunicationManager.sendPacket(): sender can not be the recipient"
        }

        connectionManager.sendPacket(
                { packetEncoder.encodePacket(packet) },
                chainID,
                recipient)
    }

    override fun broadcastPacket(packet: PacketType) {
        logger.trace { "[$processName]: broadcastPacket($packet)" }

        connectionManager.broadcastPacket(
                { packetEncoder.encodePacket(packet) },
                chainID)
    }

    override fun shutdown() {
        connectionManager.disconnectChain(chainID)
    }

    private fun decodeAndEnqueue(peerID: XPeerID, packet: ByteArray) {
        // packet decoding should not be synchronized so we can make
        // use of parallel processing in different threads
        val decodedPacket = packetDecoder.decodePacket(peerID.byteArray, packet)
        synchronized(this) {
            inboundPackets.add(peerID to decodedPacket)
        }
    }
}