// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import mu.KLogging
import net.postchain.base.BlockchainRid
import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.base.peerId
import net.postchain.common.toHex
import net.postchain.core.ProgrammerMistake
import net.postchain.debug.BlockchainProcessName
import net.postchain.devtools.PeerNameHelper.peerName
import net.postchain.network.XPacketDecoderFactory
import net.postchain.network.XPacketEncoderFactory

class DefaultXConnectionManager<PacketType>(
        private val connectorFactory: XConnectorFactory<PacketType>,
        private val packetEncoderFactory: XPacketEncoderFactory<PacketType>,
        private val packetDecoderFactory: XPacketDecoderFactory<PacketType>,
        cryptoSystem: CryptoSystem
) : XConnectionManager, XConnectorEvents {

    companion object : KLogging()

    private var connector: XConnector<PacketType>? = null
    private lateinit var peersConnectionStrategy: PeersConnectionStrategy

    // Used by connection strategy, connector and loggers (to distinguish nodes in tests' logs).
    private lateinit var myPeerInfo: PeerInfo

    private class Chain(
            val peerConfig: XChainPeerConfiguration,
            val connectAll: Boolean) {
        val connections = mutableMapOf<XPeerID, XPeerConnection>()
    }

    private val chains: MutableMap<Long, Chain> = mutableMapOf()
    private val chainIDforBlockchainRID = mutableMapOf<BlockchainRid, Long>()
    private var isShutDown = false

    override fun shutdown() {
        connector?.shutdown()
        peersConnectionStrategy.shutdown()
        synchronized(this) {
            isShutDown = true

            chains.forEach { (_, chain) ->
                chain.connections.forEach { (_, conn) -> conn.close() }
            }
            chains.clear()
        }
    }

    @Synchronized
    override fun connectChain(peerConfig: XChainPeerConfiguration, autoConnectAll: Boolean, loggingPrefix: () -> String) {
        logger.debug {
            "${loggingPrefix()}: Connecting chain: ${peerConfig.chainID}" +
                    ", blockchainRID: ${peerConfig.blockchainRID.toShortHex()}"
        }

        if (isShutDown) throw ProgrammerMistake("Already shut down")
        val chainID = peerConfig.chainID
        var ok = true
        if (chainID in chains) {
            disconnectChain(chainID, loggingPrefix)
            ok = false
        }
        chains[peerConfig.chainID] = Chain(peerConfig, autoConnectAll)
        chainIDforBlockchainRID[peerConfig.blockchainRID] = peerConfig.chainID

        // We used to create the connector at object init. But a
        // problem with initiating the connector before connecting all chains
        // is that we might close legit incoming connections that are for blockchains
        // that haven't been connected yet.
        // During startup, It'd be better to create the connector once all
        // currently known chains have been connected.
        // This solution is getting us half-way. We solve the issue for the first
        // blockchain started, but not for subsequent ones.
        if (connector == null) {
            myPeerInfo = peerConfig.commConfiguration.myPeerInfo()
            peersConnectionStrategy = DefaultPeersConnectionStrategy(this, myPeerInfo.peerId())
            connector = connectorFactory.createConnector(
                    myPeerInfo,
                    packetDecoderFactory.create(peerConfig.commConfiguration),
                    this)
        }

        if (autoConnectAll) {
            val commConf = peerConfig.commConfiguration
            peersConnectionStrategy.connectAll(chainID, commConf.networkNodes.getPeerIds())
        }

        if (!ok) throw ProgrammerMistake("Error: multiple connections to for one chain")

        logger.debug { "${logger(peerConfig)}: Chain connected: ${peerConfig.chainID}" }
    }

    private fun connectorConnectPeer(peerConfig: XChainPeerConfiguration, peerId: XPeerID) {
        logger.info { "${logger(peerConfig)}: Connecting chain peer: chain = ${peerConfig.chainID}, peer = ${peerName(peerId)}" }

        val peerConnectionDescriptor = XPeerConnectionDescriptor(
                peerId,
                peerConfig.blockchainRID)

        val peerInfo = peerConfig.commConfiguration.resolvePeer(peerId.byteArray)
                ?: throw ProgrammerMistake("Peer ID not found: ${peerId.byteArray.toHex()}")

        val packetEncoder = packetEncoderFactory.create(
                peerConfig.commConfiguration,
                peerConfig.blockchainRID)

        connector?.connectPeer(peerConnectionDescriptor, peerInfo, packetEncoder)
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
        chain.connections.forEach { (_, conn) ->
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
    override fun disconnectChain(chainID: Long, loggingPrefix: () -> String) {
        logger.debug { "${loggingPrefix()}: Disconnecting chain: $chainID" }

        // Remove the chain before closing connections so that we won't
        // reconnect in onPeerDisconnected()
        val chain = chains.remove(chainID)
        if (chain != null) {
            chain.connections.forEach { (_, conn) ->
                conn.close()
            }
            chain.connections.clear()
            logger.debug { "${loggingPrefix()}: Chain disconnected: $chainID" }

        } else {
            logger.debug { "${loggingPrefix()}: Unknown chain: $chainID" }
        }
    }

    @Synchronized
    override fun onPeerConnected(connection: XPeerConnection): XPacketHandler? {
        val descriptor = connection.descriptor()
        logger.info {
            "${logger(descriptor)}: New ${descriptor.dir} connection: peer = ${peerName(descriptor.peerId)}" +
                    ", blockchainRID: ${descriptor.blockchainRID}" +
                    ", (size of c4Brid: ${chainIDforBlockchainRID.size}, size of chains: ${chains.size}) "
        }

        val chainID = chainIDforBlockchainRID[descriptor.blockchainRID]
        if (chainID == null) {
            logger.warn("${logger(descriptor)}: onPeerConnected: Chain ID not found by blockchainRID = ${descriptor.blockchainRID}")
            connection.close()
            return null
        }
        val chain = chains[chainID]
        if (chain == null) {
            logger.warn("${logger(descriptor)}: onPeerConnected: Chain not found by chainID = ${chainID}} / blockchainRID = ${descriptor.blockchainRID}")
            connection.close()
            return null
        }

        return if (!chain.peerConfig.commConfiguration.networkNodes.isNodeBehavingWell(descriptor.peerId, System.currentTimeMillis())) {
            logger.debug { "${logger(descriptor)}: onPeerConnected: Peer not behaving well, so ignore: peer = ${peerName(descriptor.peerId)}" }
            null
        } else {
            val originalConn = chain.connections[descriptor.peerId]
            if (originalConn != null) {
                logger.debug { "${logger(descriptor)}: onPeerConnected: Peer already connected: peer = ${peerName(descriptor.peerId)}" }
                val isOriginalOutgoing = originalConn.descriptor().isOutgoing()
                if (peersConnectionStrategy.duplicateConnectionDetected(chainID, isOriginalOutgoing, descriptor.peerId)) {
                    disconnectChainPeer(chainID, descriptor.peerId)
                    chain.connections[descriptor.peerId] = connection
                    logger.debug { "${logger(descriptor)}: onPeerConnected: Peer connected and replaced previous connection: peer = ${peerName(descriptor.peerId)}" }
                    chain.peerConfig.packetHandler
                } else {
                    connection.close()
                    null
                }
            } else {
                chain.connections[descriptor.peerId] = connection
                logger.debug { "${logger(descriptor)}: onPeerConnected: Connection accepted: peer = ${peerName(descriptor.peerId)}" }
                peersConnectionStrategy.connectionEstablished(chainID, connection.descriptor().isOutgoing(), descriptor.peerId)
                chain.peerConfig.packetHandler
            }
        }
    }

    @Synchronized
    override fun onPeerDisconnected(connection: XPeerConnection) {
        val descriptor = connection.descriptor()
        logger.debug {
            "${logger(descriptor)}: Peer disconnected: peer = ${peerName(descriptor.peerId)}" +
                    ", direction: ${descriptor.dir}"
        }

        val chainID = chainIDforBlockchainRID[descriptor.blockchainRID]
        val chain = if (chainID != null) chains[chainID] else null
        if (chain == null) {
            logger.warn("${logger(descriptor)}: Peer disconnected: chain not found by blockchainRID = ${descriptor.blockchainRID} / chainID = $chainID")
            return
        }

        if (chain.connections[descriptor.peerId] == connection) {
            // It's the connection we're using, so we have to remove it
            chain.connections.remove(descriptor.peerId)
        }

        if (chain.connectAll) {
            peersConnectionStrategy.connectionLost(chainID!!, descriptor.peerId, descriptor.isOutgoing())
        }
    }

    override fun getPeersTopology(): Map<String, Map<String, String>> {
        return chains
                .mapKeys { (id, chain) -> id to chain.peerConfig.blockchainRID.toHex() }
                .mapValues { (idToRid, _) -> getPeersTopology(idToRid.first).mapKeys { (k, _) -> k.toString() } }
                .mapKeys { (idToRid, _) -> idToRid.second }
    }

    override fun getPeersTopology(chainID: Long): Map<XPeerID, String> {
        return chains[chainID]
                ?.connections
                ?.mapValues { connection ->
                    (if (connection.value.descriptor().isOutgoing()) "c-s" else "s-c") + ", " + connection.value.remoteAddress()
                }
                ?: emptyMap()
    }

    private fun loggingPrefix(blockchainRid: BlockchainRid): String = BlockchainProcessName(
            myPeerInfo.peerId().toString(),
            blockchainRid
    ).toString()

    private fun logger(descriptor: XPeerConnectionDescriptor): String = loggingPrefix(descriptor.blockchainRID)

    private fun logger(config: XChainPeerConfiguration): String = loggingPrefix(config.blockchainRID)
}