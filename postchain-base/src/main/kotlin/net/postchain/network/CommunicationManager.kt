// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network

import net.postchain.network.x.XPeerID

interface CommunicationManager<PacketType> {
    fun init()
    //fun peerMap(): Map<XPeerID, PeerInfo>
    fun getPackets(): MutableList<Pair<XPeerID, PacketType>>
    fun sendPacket(packet: PacketType, recipient: XPeerID)
    fun broadcastPacket(packet: PacketType)
    /**
     * Sends the packet to a peer selected by random.
     *
     * @param amongPeers consider only these peers. The random choice will thus be made from the intersection of
     * amongPeers and connected peers.
     * @return the selected peer that the packet was sent to. If there
     * were no peers available, null is returned.
     */
    fun sendToRandomPeer(packet: PacketType, amongPeers: Set<XPeerID>): XPeerID?
    fun shutdown()
}
