package net.postchain.network

interface IdentPacketConverter {
    fun makeIdentPacket(forPeer: PeerID): ByteArray
    fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo
}