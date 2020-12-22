// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import mu.KLogging
import net.postchain.base.BlockchainRid
import net.postchain.base.PeerCommConfiguration
import net.postchain.common.toHex
import net.postchain.core.BadDataMistake
import net.postchain.core.BadDataType
import net.postchain.debug.BlockchainProcessName
import net.postchain.devtools.PeerNameHelper.peerName
import net.postchain.network.CommunicationManager
import net.postchain.network.XPacketDecoder
import net.postchain.network.XPacketEncoder

class DefaultXCommunicationManager<PacketType>(
        val connectionManager: XConnectionManager,
        val config: PeerCommConfiguration,
        val chainID: Long,
        val blockchainRID: BlockchainRid,
        private val packetEncoder: XPacketEncoder<PacketType>,
        private val packetDecoder: XPacketDecoder<PacketType>,
        private val processName: BlockchainProcessName
) : CommunicationManager<PacketType> {

    companion object : KLogging()

    private var inboundPackets = mutableListOf<Pair<XPeerID, PacketType>>()

    override fun init() {
        val peerConfig = XChainPeerConfiguration(
                chainID,
                blockchainRID,
                config,
                { data: ByteArray, peerID: XPeerID -> decodeAndEnqueue(peerID, data) }
        )

        connectionManager.connectChain(peerConfig, true) { processName.toString() }
    }

    @Synchronized
    override fun getPackets(): MutableList<Pair<XPeerID, PacketType>> {
        val currentQueue = inboundPackets
        inboundPackets = mutableListOf()
        return currentQueue
    }

    override fun sendPacket(packet: PacketType, recipient: XPeerID) {
        logger.trace { "$processName: sendPacket($packet, ${peerName(recipient.toString())})" }

        require(XPeerID(config.pubKey) != recipient) {
            "CommunicationManager.sendPacket(): sender can not be the recipient"
        }

        connectionManager.sendPacket(
                { packetEncoder.encodePacket(packet) },
                chainID,
                recipient)
    }

    override fun broadcastPacket(packet: PacketType) {
        logger.trace { "$processName: broadcastPacket($packet)" }

        connectionManager.broadcastPacket(
                { packetEncoder.encodePacket(packet) },
                chainID)
    }

    override fun sendToRandomPeer(packet: PacketType, excludedPeers: Set<XPeerID>): XPeerID? {
        try {
            val peer = connectionManager.getConnectedPeers(chainID).minus(excludedPeers).random()
            logger.trace { "$processName: sendToRandomPeer($packet, ${peerName(peer.toString())})" }
            sendPacket(packet, peer)
            return peer
        } catch (e: Exception) {
            return null
        }
    }

    override fun shutdown() {
        connectionManager.disconnectChain(chainID) { processName.toString() }
    }

    private fun decodeAndEnqueue(peerID: XPeerID, packet: ByteArray) {
        try {
            // packet decoding should not be synchronized so we can make
            // use of parallel processing in different threads
            logger.trace("receiving a packet from peer: ${peerID.byteArray.toHex()}")
            val decodedPacket = packetDecoder.decodePacket(peerID.byteArray, packet)
            synchronized(this) {
                logger.trace("Successfully decoded the package, now adding it ")
                inboundPackets.add(peerID to decodedPacket)
            }
        } catch (e: BadDataMistake) {
            if (e.type == BadDataType.BAD_MESSAGE) {
                logger.info("Bad message received from peer ${peerID}: ${e.message}")
            } else {
                logger.error("Error when receiving message from peer ${peerID}", e)
            }
        }
    }
}