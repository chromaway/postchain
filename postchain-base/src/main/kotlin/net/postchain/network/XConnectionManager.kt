package net.postchain.network

import net.postchain.base.PeerCommConfiguration
import net.postchain.core.ByteArrayKey
import net.postchain.core.Shutdownable

typealias XPeerID = ByteArrayKey
typealias XPacketHandler = (data: ByteArray, peerID: XPeerID) -> Unit

typealias LazyPacket = () -> ByteArray

/* TODO: merge with PeerCommConfiguration */
class XChainPeerConfiguration(
        val chainID: Long,
        val commConfiguration: PeerCommConfiguration,
        val packetHandler: XPacketHandler
)

interface XConnectionManager: Shutdownable {
    fun sendPacket(data: LazyPacket, chainID: Long, peerID: XPeerID)
    fun broadcastPacket(data: LazyPacket, chainID: Long)

    fun connectChain(peerConfig: XChainPeerConfiguration,
                     autoConnectAll: Boolean)

    fun connectChainPeer(chainID: Long, peerID: XPeerID)
    fun isPeerConnected(chainID: Long, peerID: XPeerID): Boolean
    fun disconnectChainPeer(chainID: Long, peerID: XPeerID)
    fun disconnectChain(chainID: Long)
    fun getConnectedPeers(chainID: Long): List<XPeerID>
}