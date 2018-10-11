package net.postchain.network

import net.postchain.base.PeerID
import net.postchain.core.ByteArrayKey

interface PacketConverter<PacketType> : IdentPacketConverter {
    fun decodePacket(pubKey: ByteArray, bytes: ByteArray): PacketType
    fun encodePacket(packet: PacketType): ByteArray
}

data class OutboundPacket<PacketType>(val packet: PacketType, val recipients: List<ByteArrayKey>)

data class IdentPacketInfo(val peerID: PeerID, val blockchainRID: ByteArray)
