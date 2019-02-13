package net.postchain.network

import net.postchain.base.PeerInfo
import net.postchain.network.x.XPeerID

interface CommunicationManager<PacketType> {
    fun peers(): Array<PeerInfo>
    fun getPackets(): MutableList<Pair<XPeerID, PacketType>>
    fun sendPacket(packet: PacketType, recipients: Set<Int>)
    fun broadcastPacket(packet: PacketType)
    fun getPeerIndex(peerID: XPeerID): Int
}
