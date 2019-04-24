package net.postchain.network

import net.postchain.base.PeerID

@Deprecated("Deprecated after Netty2")
interface IdentPacketConverter {
    fun makeIdentPacket(forPeer: PeerID): ByteArray
    fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo
}