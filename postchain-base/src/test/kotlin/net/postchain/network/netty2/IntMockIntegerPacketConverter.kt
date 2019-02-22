package net.postchain.network.netty2

import net.postchain.base.PeerID
import net.postchain.base.PeerInfo
import net.postchain.network.IdentPacketInfo
import net.postchain.network.PacketConverter
import java.nio.ByteBuffer

@Deprecated("Delete it")
class IntMockIntegerPacketConverter(
        private val ownerPeerInfo: PeerInfo,
        private val peerInfos: Array<PeerInfo>
) : PacketConverter<Int> {

    override fun decodePacket(pubKey: ByteArray, bytes: ByteArray): Int {
        return ByteBuffer.wrap(bytes).int
    }

    override fun decodePacket(bytes: ByteArray): Int? {
        return ByteBuffer.wrap(bytes).int
    }

    override fun encodePacket(packet: Int): ByteArray {
        return ByteBuffer.allocate(4).putInt(packet).array()
    }

    // TODO: [et]: This logic corresponds to the [EbftPacketConverter]'s one (ignore [forPeer] here)
    override fun makeIdentPacket(forPeer: PeerID): ByteArray {
        return ownerPeerInfo.pubKey
    }

    // TODO: [et]: This logic corresponds to the [EbftPacketConverter]'s one
    override fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo {
        return IdentPacketInfo(bytes, byteArrayOf())
    }

    override fun isIdentPacket(bytes: ByteArray): Boolean {
        return peerInfos.any { it.pubKey.contentEquals(bytes) }
    }
}