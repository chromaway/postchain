package net.postchain.network.x

import mu.KLogging
import net.postchain.base.CryptoSystem
import net.postchain.base.PeerCommConfiguration
import net.postchain.common.ExponentialDelay
import net.postchain.common.toHex
import net.postchain.core.ByteArrayKey
import net.postchain.core.ProgrammerMistake
import net.postchain.core.byteArrayKeyOf
import net.postchain.devtools.PeerNameHelper.peerName
import net.postchain.network.XPacketDecoderFactory
import net.postchain.network.XPacketEncoderFactory
import net.postchain.network.netty2.NettyClientPeerConnection
import nl.komponents.kovenant.task
import java.util.*
import kotlin.concurrent.schedule

class DefaultXConnectionManager<PacketType>(
        connectorFactory: XConnectorFactory<PacketType>,
        val peerCommConfiguration: PeerCommConfiguration,
        private val packetEncoderFactory: XPacketEncoderFactory<PacketType>,
        private val packetDecoderFactory: XPacketDecoderFactory<PacketType>,
        cryptoSystem: CryptoSystem,
        private val peersConnectionStrategy: PeersConnectionStrategy = DefaultPeersConnectionStrategy
) : XConnectionManager, NetworkTopology, XConnectorEvents {

    private val connector = connectorFactory.createConnector(
            peerCommConfiguration.myPeerInfo(),
            packetDecoderFactory.create(peerCommConfiguration),
            this)

    companion object : KLogging()

    private class Chain(
            val peerConfig: XChainPeerConfiguration,
            val connectAll: Boolean) {
        val neededConnections = mutableSetOf<XPeerID>()
        val connections = mutableMapOf<XPeerID, XPeerConnection>()
    }

    private val chains: MutableMap<Long, Chain> = mutableMapOf()
    private val chainIDforBlockchainRID = mutableMapOf<ByteArrayKey, Long>()
    private var isShutDown = false

    private val peerToDelayMap: MutableMap<XPeerID, ExponentialDelay> = mutableMapOf()


    @Synchronized
    override fun shutdown() {
        isShutDown = true

        chains.forEach { (_, chain) ->
            chain.connections.forEach { (_, conn) -> conn.close() }
        }
        chains.clear()

        connector.shutdown()
    }

    @Synchronized
    override fun connectChain(peerConfig: XChainPeerConfiguration, autoConnectAll: Boolean) {
        logger.debug {
            "[${myPeerId()}]: Connecting chain: ${peerConfig.chainID} " +
                    "BcRID: ${peerConfig.blockchainRID.toHex()}"
        }

        if (isShutDown) throw ProgrammerMistake("Already shut down")
        val chainID = peerConfig.chainID
        var ok = true
        if (chainID in chains) {
            disconnectChain(chainID)
            ok = false
        }
        chains[peerConfig.chainID] = Chain(peerConfig, autoConnectAll)
        chainIDforBlockchainRID[peerConfig.blockchainRID.byteArrayKeyOf()] =
                peerConfig.chainID

        if (autoConnectAll) {
            peersConnectionStrategy.forEach(peerConfig.commConfiguration) {
                connectorConnectPeer(peerConfig, it.pubKey.byteArrayKeyOf())
            }
        }

        if (!ok) throw ProgrammerMistake("Error: multiple connections to for one chain")

        logger.debug { "[${myPeerId()}]: Chain connected: ${peerConfig.chainID}" }
    }

    private fun connectorConnectPeer(peerConfig: XChainPeerConfiguration, peerId: XPeerID) {
        logger.debug { "[${myPeerId()}]: Connecting chain peer: chain = ${peerConfig.chainID}, peer = ${peerName(peerId)}" }

        val peerConnectionDescriptor = XPeerConnectionDescriptor(
                peerId,
                peerConfig.blockchainRID.byteArrayKeyOf())

        val peerInfo = peerConfig.commConfiguration.resolvePeer(peerId.byteArray)
                ?: throw ProgrammerMistake("Peer ID not found: ${peerId.byteArray.toHex()}")

        val packetEncoder = packetEncoderFactory.create(
                peerConfig.commConfiguration,
                peerConfig.blockchainRID)

        task {
            connector.connectPeer(peerConnectionDescriptor, peerInfo, packetEncoder)
            logger.debug { "[${myPeerId()}]: Chain peer connected: chain = ${peerConfig.chainID}, peer = $peerId" }
        }
    }

    @Synchronized
    override fun connectChainPeer(chainID: Long, peerID: XPeerID) {
        val chain = chains[chainID] ?: throw ProgrammerMistake("Chain ID not found: $chainID")
        if (peerID !in chain.connections) { // ignore if already connected
            connectorConnectPeer(chain.peerConfig, peerID)
        }
    }

    @Synchronized
    override fun isPeerConnected(chainID: Long, peerID: XPeerID): Boolean {
        val chain = chains[chainID] ?: throw ProgrammerMistake("Chain ID not found: $chainID")
        return peerID in chain.connections
    }

    @Synchronized
    override fun getConnectedPeers(chainID: Long): List<XPeerID> {
        val chain = chains[chainID] ?: throw ProgrammerMistake("Chain ID not found: $chainID")
        return chain.connections.keys.toList()
    }

    @Synchronized
    override fun sendPacket(data: LazyPacket, chainID: Long, peerID: XPeerID) {
        val chain = chains[chainID] ?: throw ProgrammerMistake("Chain ID not found: $chainID")
        chain.connections[peerID]?.sendPacket(data)
    }

    @Synchronized
    override fun broadcastPacket(data: LazyPacket, chainID: Long) {
        // TODO: lazypacket might be computed multiple times
        val chain = chains[chainID] ?: throw ProgrammerMistake("Chain ID not found: $chainID")
        chain.connections.forEach { _, conn ->
            conn.sendPacket(data)
        }
    }

    @Synchronized
    override fun disconnectChainPeer(chainID: Long, peerID: XPeerID) {
        val chain = chains[chainID] ?: throw ProgrammerMistake("Chain ID not found: $chainID")
        val conn = chain.connections[peerID]
        if (conn != null) {
            conn.close()
            chain.connections.remove(peerID)
        }
    }

    @Synchronized
    override fun disconnectChain(chainID: Long) {
        logger.debug { "[${myPeerId()}]: Disconnecting chain: $chainID" }

        val chain = chains[chainID]
        if (chain != null) {
            chain.connections.forEach { _, conn ->
                conn.close()
            }
            chain.connections.clear()
            chains.remove(chainID)
            logger.debug { "[${myPeerId()}]: Chain disconnected: $chainID" }

        } else {
            logger.debug { "[${myPeerId()}]: Unknown chain: $chainID" }
        }
    }

    @Synchronized
    override fun onPeerConnected(descriptor: XPeerConnectionDescriptor, connection: XPeerConnection): XPacketHandler? {
        logger.debug {
            "[${myPeerId()}]: onPeerConnected: peerId = ${peerName(descriptor.peerId)}, " +
                    "connection = ${connection.javaClass.simpleName}, BcRID: ${descriptor.blockchainRID}"
        }

        val chainID = chainIDforBlockchainRID[descriptor.blockchainRID]
        val chain = if (chainID != null) chains[chainID] else null
        if (chain == null) {
            logger.warn("[${myPeerId()}]: Chain not found by blockchainRID = ${descriptor.blockchainRID} / chainID = $chainID")
            connection.close()
            return null
        }

        return if (!peerCommConfiguration.networkNodes.isNodeBehavingWell(descriptor.peerId, System.currentTimeMillis())) {
            logger.debug { "[${myPeerId()}]: Peer not behaving well, so ignore: peerId = ${peerName(descriptor.peerId)}" }
            null
        } else if (chain.connections[descriptor.peerId] != null) {
            logger.debug { "[${myPeerId()}]: Peer already connected: peerId = ${peerName(descriptor.peerId)}" }
            null
        } else {
            chain.connections[descriptor.peerId] = connection
            logger.debug { "[${myPeerId()}]: Peer connected: peerId = ${peerName(descriptor.peerId)}" }
            peerToDelayMap.remove(descriptor.peerId) // We are connected, with means we must clear the re-connect delay
            chain.peerConfig.packetHandler
        }
    }

    @Synchronized
    override fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor, connection: XPeerConnection) {
        logger.debug { "[${myPeerId()}]: onPeerDisconnected: peerId = ${peerName(descriptor.peerId)}" }

        val chainID = chainIDforBlockchainRID[descriptor.blockchainRID]
        val chain = if (chainID != null) chains[chainID] else null
        if (chain == null) {
            logger.warn("[${myPeerId()}]: Chain not found by blockchainRID = ${descriptor.blockchainRID} / chainID = $chainID")
            return
        }

        val oldConnection = chain.connections[descriptor.peerId]
        if (oldConnection != null) {
            oldConnection.close()
            chain.connections.remove(descriptor.peerId)
        }

        // Reconnecting if connectionType is CLIENT
        if (connection is NettyClientPeerConnection<*>) {
            if (chain.connectAll || (descriptor.peerId in chain.neededConnections)) {
                reconnect(chain.peerConfig, descriptor.peerId)
            }
        }

        logger.debug { "[${myPeerId()}]: Peer disconnected: peerId = ${peerName(descriptor.peerId)}" }
    }

    override fun getPeersTopology(chainID: Long): Map<XPeerID, String> {
        return chains[chainID]
                ?.connections
                ?.mapValues { peerToConnection ->
                    when (peerToConnection.value) {
                        is NettyClientPeerConnection<*> -> "c>s" // TODO: Fix this
                        else -> "s<c" // TODO: Fix this
                    }
                }
                ?: emptyMap()
    }

    private fun reconnect(peerConfig: XChainPeerConfiguration, peerId: XPeerID) {
        // Make sure there is an [ExponentialDelay] instance for this peer
        var delay = peerToDelayMap[peerId]
        if (delay == null) {
            delay = ExponentialDelay()
            peerToDelayMap[peerId] = delay
        }

        Timer("Reconnecting").schedule(delay.getDelayMillis()) {
            logger.debug { "[${myPeerId()}]: Reconnecting to peer: peerId = ${peerName(peerId)}" }
            connectorConnectPeer(peerConfig, peerId)
        }
    }

    private fun myPeerId(): String =
            peerName(peerCommConfiguration.myPeerInfo().pubKey)
}