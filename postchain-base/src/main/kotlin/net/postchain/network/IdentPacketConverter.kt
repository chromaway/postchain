package net.postchain.network

import net.postchain.base.PeerID

interface IdentPacketConverter {
    fun makeIdentPacket(forPeer: PeerID): ByteArray
    fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo
}