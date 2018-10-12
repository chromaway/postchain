package net.postchain.network

import net.postchain.base.PeerCommConfiguration
import net.postchain.core.ByteArrayKey
import net.postchain.core.Shutdownable

typealias XPeerID = ByteArrayKey
typealias XPacketHandler = (data: ByteArray, peerID: XPeerID) -> Unit

typealias LazyPacket = () -> ByteArray

/* TODO: merge with PeerCommConfiguration */
class XChainPeerConfiguration (
        val chainID: Long,
        val commConfiguration: PeerCommConfiguration,
        val packetHandler: XPacketHandler,
        /* this implies that conn manager handles authentication in
        a particular way and puts the burden of authentication into higher
        level protocols. this is not good, and in future we will make
        conn manager fully responsible for authentication process
        */
        val identPacketConverter: IdentPacketConverter
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