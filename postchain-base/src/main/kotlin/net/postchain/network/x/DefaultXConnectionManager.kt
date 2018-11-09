package net.postchain.network.x

import mu.KLogging
import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.common.toHex
import net.postchain.core.ByteArrayKey
import net.postchain.core.ProgrammerMistake
import net.postchain.core.byteArrayKeyOf
import net.postchain.network.IdentPacketConverter

class DefaultXConnectionManager(
        connectorFactory: XConnectorFactory,
        myPeerInfo: PeerInfo,
        identPacketConverter: IdentPacketConverter,
        cryptoSystem: CryptoSystem
) : XConnectionManager, XConnectorEvents {

    private val connector = connectorFactory.createConnector(
            myPeerInfo, identPacketConverter, this, cryptoSystem)

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

    @Synchronized
    override fun shutdown() {
        isShutDown = true
        chains.forEach { _, chain ->
            chain.connections.forEach { _, conn -> conn.close() }
        }
        chains.clear()
    }

    @Synchronized
    override fun connectChain(peerConfig: XChainPeerConfiguration, autoConnectAll: Boolean) {
        if (isShutDown) throw ProgrammerMistake("Already shut down")
        val chainID = peerConfig.chainID
        var ok = true
        if (chainID in chains) {
            disconnectChain(chainID)
            ok = false
        }
        chains[peerConfig.chainID] = Chain(peerConfig, autoConnectAll)
        chainIDforBlockchainRID[peerConfig.commConfiguration.blockchainRID.byteArrayKeyOf()] =
                peerConfig.chainID

        if (autoConnectAll) {
            peerConfig.commConfiguration.othersPeerInfo().forEach {
                connectorConnectPeer(peerConfig, it.pubKey.byteArrayKeyOf())
            }
        }

        if (!ok) throw ProgrammerMistake("Error: multiple connections to for one chain")
    }

    private fun connectorConnectPeer(peerConfig: XChainPeerConfiguration, peerID: XPeerID) {
        val peerInfo = peerConfig.commConfiguration.resolvePeer(peerID.byteArray)
                ?: throw ProgrammerMistake("Peer ID not found: ${peerID.byteArray.toHex()}")

        connector.connectPeer(
                XPeerConnectionDescriptor(
                        peerID,
                        peerConfig.commConfiguration.blockchainRID.byteArrayKeyOf()
                ),
                peerInfo
        )
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
        val chain = chains[chainID]
        if (chain != null) {
            chain.connections.forEach { _, conn ->
                conn.close()
            }
            chain.connections.clear()
            chains.remove(chainID)
        }
    }

    @Synchronized
    override fun onPeerConnected(descriptor: XPeerConnectionDescriptor, connection: XPeerConnection): XPacketHandler? {
        val chainID = chainIDforBlockchainRID[descriptor.blockchainRID]
        val chain = if (chainID != null) chains[chainID] else null
        if (chain == null) {
            logger.warn("Chain not found")
            connection.close()
            return null
        }

        // TODO: test if connection is wanted

        chain.connections[descriptor.peerID]?.close()
        chain.connections[descriptor.peerID] = connection
        return chain.peerConfig.packetHandler
    }

    @Synchronized
    override fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor) {
        val chainID = chainIDforBlockchainRID[descriptor.blockchainRID]
        val chain = if (chainID != null) chains[chainID] else null
        if (chain == null) {
            logger.warn("Chain not found")
            return
        }
        val oldConnection = chain.connections[descriptor.peerID]
        if (oldConnection != null) {
            oldConnection.close()
            chain.connections.remove(descriptor.peerID)
        }

        if (chain.connectAll || (descriptor.peerID in chain.neededConnections)) {
            connectorConnectPeer(chain.peerConfig, descriptor.peerID)
        }
    }
}