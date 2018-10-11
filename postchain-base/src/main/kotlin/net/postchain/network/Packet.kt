package net.postchain.network

import net.postchain.base.PeerID
import net.postchain.core.ByteArrayKey

interface PacketConverter<PT> : IdentPacketConverter {
    fun decodePacket(pubKey: ByteArray, bytes: ByteArray): PT
    fun encodePacket(packet: PT): ByteArray
}

data class OutboundPacket<PT>(val packet: PT, val recipients: List<ByteArrayKey>)

data class IdentPacketInfo(val peerID: PeerID, val blockchainRID: ByteArray)
