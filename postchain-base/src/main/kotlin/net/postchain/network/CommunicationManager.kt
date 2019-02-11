package net.postchain.network

import net.postchain.base.PeerInfo

interface CommunicationManager<PacketType> {
    fun peers(): Array<PeerInfo>
    fun getPackets(): MutableList<Pair<Int, PacketType>>
    fun sendPacket(packet: PacketType, recipients: Set<Int>)
    fun broadcastPacket(packet: PacketType)
}
