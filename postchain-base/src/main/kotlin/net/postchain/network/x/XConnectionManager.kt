package net.postchain.network.x

import net.postchain.core.ByteArrayKey
import net.postchain.core.Shutdownable

typealias XPeerID = ByteArrayKey
typealias XPacketHandler = (data: ByteArray, peerID: XPeerID) -> Unit

typealias LazyPacket = () -> ByteArray

interface XConnectionManager : Shutdownable {
    fun connectChain(peerConfig: XChainPeerConfiguration, autoConnectAll: Boolean)
    fun connectChainPeer(chainID: Long, peerID: XPeerID)
    fun isPeerConnected(chainID: Long, peerID: XPeerID): Boolean
    fun getConnectedPeers(chainID: Long): List<XPeerID>
    fun sendPacket(data: LazyPacket, chainID: Long, peerID: XPeerID)
    fun broadcastPacket(data: LazyPacket, chainID: Long)
    fun disconnectChainPeer(chainID: Long, peerID: XPeerID)
    fun disconnectChain(chainID: Long)
}