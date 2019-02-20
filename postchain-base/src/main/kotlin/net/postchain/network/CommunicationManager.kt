package net.postchain.network

import net.postchain.base.PeerInfo
import net.postchain.network.x.XPeerID

interface CommunicationManager<PacketType> {
    fun init()
    fun peers(): Array<PeerInfo>
    fun getPackets(): MutableList<Pair<XPeerID, PacketType>>
    fun sendPacket(packet: PacketType, recipient: XPeerID)
    fun broadcastPacket(packet: PacketType)
}
