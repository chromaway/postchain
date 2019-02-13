package net.postchain.network.x

import mu.KLogging
import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.core.NODE_ID_READ_ONLY
import net.postchain.core.Shutdownable
import net.postchain.core.byteArrayKeyOf
import net.postchain.network.CommunicationManager
import net.postchain.network.PacketConverter

class DefaultXCommunicationManager<PacketType>(
        val connectionManager: XConnectionManager,
        val config: PeerCommConfiguration,
        val chainID: Long,
        val packetConverter: PacketConverter<PacketType>
) : CommunicationManager<PacketType>, Shutdownable {

    override fun getPeerIndex(peerID: XPeerID): Int {
        return config.peerInfo.map { XPeerID(it.pubKey) }.indexOf(peerID)
    }

    companion object : KLogging()

    private var inboundPackets = mutableListOf<Pair<XPeerID, PacketType>>()

    override fun init() {
        val peerConfig = XChainPeerConfiguration(
                chainID,
                config,
                { data: ByteArray, peerID: XPeerID -> decodeAndEnqueue(peerID, data) },
                packetConverter)

        connectionManager.connectChain(peerConfig, true)
    }

    override fun peers(): Array<PeerInfo> = config.peerInfo

    @Synchronized
    override fun getPackets(): MutableList<Pair<XPeerID, PacketType>> {
        val currentQueue = inboundPackets
        inboundPackets = mutableListOf()
        return currentQueue
    }

    override fun sendPacket(packet: PacketType, recipients: Set<Int>) {
        require(recipients.size == 1) {
            "CommunicationManager.sendPacket(): multiple recipients are not allowed"
        }

        require(recipients.first() in config.peerInfo.indices) {
            "CommunicationManager.sendPacket(): recipient must be in range ${config.peerInfo.indices}"
        }

        require(recipients.first() != config.myIndex) {
            "CommunicationManager.sendPacket(): recipient must not be equal to myIndex ${config.myIndex}"
        }

        val peerIdx = recipients.first()
        connectionManager.sendPacket(
                { packetConverter.encodePacket(packet) },
                chainID,
                config.peerInfo[peerIdx].pubKey.byteArrayKeyOf())
    }

    override fun broadcastPacket(packet: PacketType) {
        connectionManager.broadcastPacket(
                { packetConverter.encodePacket(packet) },
                chainID)
    }

    override fun shutdown() {
        connectionManager.disconnectChain(chainID)
    }

    private fun decodeAndEnqueue(peerID: XPeerID, packet: ByteArray) {
        // packet decoding should not be synchronized so we can make
        // use of parallel processing in different threads
        val decodedPacket = packetConverter.decodePacket(peerID.byteArray, packet)
        synchronized(this) {
            inboundPackets.add(peerID to decodedPacket)
        }
    }
}