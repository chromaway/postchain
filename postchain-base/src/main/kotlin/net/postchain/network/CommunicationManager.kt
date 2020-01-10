package net.postchain.network

import net.postchain.base.PeerInfo
import net.postchain.core.Shutdownable
import net.postchain.network.x.XPeerID

interface CommunicationManager<PacketType> : Shutdownable {
    fun init()
    //fun peerMap(): Map<XPeerID, PeerInfo>
    fun getPackets(): MutableList<Pair<XPeerID, PacketType>>
    fun sendPacket(packet: PacketType, recipient: XPeerID)
    fun broadcastPacket(packet: PacketType)
}
