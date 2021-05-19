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
import java.lang.IllegalStateException

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

    private fun updateBridToChainIDCache(blockchainRid: BlockchainRid, peerConfig: XChainPeerConfiguration) {
        val foundChainId = chainIDforBlockchainRID[blockchainRid]
        if (foundChainId == null) {
            chainIDforBlockchainRID[blockchainRid] = peerConfig.chainID
        } else {
            if (foundChainId != peerConfig.chainID) {
                throw ProgrammerMistake("Chain ${peerConfig.blockchainRID} cannot be connected to ${peerConfig.chainID} is connected to a different chain: $foundChainId. ")
            }
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
        if (chainID in chains) {
            throw ProgrammerMistake("Chain is already connected ${chainID}")
        }
        updateBridToChainIDCache(peerConfig.blockchainRID, peerConfig)
        chains[peerConfig.chainID] = Chain(peerConfig, autoConnectAll)

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

        logger.debug { "${logger(peerConfig)}: Chain connected: ${peerConfig.chainID}" }
    }

    private fun connectorConnectPeer(peerConfig: XChainPeerConfiguration, peerId: XPeerID) {
        logger.info { "${logger(peerConfig)}: Connecting chain peer: chain = ${peerConfig.chainID}, peer = ${peerName(peerId)}" }

        val peerConnectionDescriptor = XPeerConnectionDescriptor(
                peerId,
                peerConfig.blockchainRID)

        val peerInfo = peerConfig.commConfiguration.resolvePeer(peerId.byteArray)
                ?: throw ProgrammerMistake("Peer ID not found: ${peerId.byteArray.toHex()}")
        if (peerInfo.peerId() != peerId) {
            // Have to add this check since I see strange things
            throw IllegalStateException("Peer id found in comm config not same as we looked for ${peerId.byteArray.toHex()} +" +
                    ", found: ${peerInfo.peerId().byteArray.toHex()} ")
        }

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
            chainIDforBlockchainRID.remove(chain.peerConfig.blockchainRID)
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
            logger.warn("${logger(descriptor)}: onPeerConnected: Chain not found by chainID = $chainID / blockchainRID = ${descriptor.blockchainRID}. " +
                    "(This is expected if it happens after this chain was restarted).")
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

    /**
     * We often don't know why we got a disconnect.
     * It could be because we did "disconnectChain()" ourselves, and for those cases we don't even have the BC is chain[].
     */
    @Synchronized
    override fun onPeerDisconnected(connection: XPeerConnection) {
        val descriptor = connection.descriptor()

        val chainID = chainIDforBlockchainRID[descriptor.blockchainRID]
        if (chainID == null) {
            logger.warn("${descriptor.loggingPrefix(myPeerInfo.peerId())}: Peer disconnected: Why can't we find chainID? peer: ${peerName(descriptor.peerId)} " +
                    ", direction: ${descriptor.dir}, blockchainRID = ${descriptor.blockchainRID} / chainID = $chainID.\") . ")
            connection.close()
            return
        }
        val chain = chains[chainID]
        if (chain == null) {
            // This is not an error
            logger.debug("${descriptor.loggingPrefix(myPeerInfo.peerId())}: Peer disconnected: chain structure gone, probably "+
                    " removed by disconnectChain(). peer: ${peerName(descriptor.peerId)} " +
                    ", direction: ${descriptor.dir}, blockchainRID = ${descriptor.blockchainRID} / chainID = $chainID.\") . ")
            connection.close()
            return
        }

        if (chain.connections[descriptor.peerId] == connection) {
            logger.debug("${descriptor.loggingPrefix(myPeerInfo.peerId())}: Peer disconnected: Removing peer: ${peerName(descriptor.peerId)}" +
                                        ", direction: ${descriptor.dir} from blockchainRID = ${descriptor.blockchainRID} / chainID = $chainID.")
            // It's the connection we're using, so we have to remove it
            chain.connections.remove(descriptor.peerId)
        }
        connection.close()
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

    private fun logger(descriptor: XPeerConnectionDescriptor): String = descriptor.loggingPrefix(myPeerInfo.peerId())

    private fun logger(config: XChainPeerConfiguration): String = loggingPrefix(config.blockchainRID)
}