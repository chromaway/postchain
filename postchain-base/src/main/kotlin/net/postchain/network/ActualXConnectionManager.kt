package net.postchain.network

import net.postchain.core.ByteArrayKey
import net.postchain.core.ProgrammerMistake
import net.postchain.core.byteArrayKeyOf


abstract class ActualXConnectionManager(
        val connector: XConnector
) : XConnectionManager, XConnectorEvents
{

    private class Chain (
            val peerConfig: XChainPeerConfiguration,
            val connectAll: Boolean) {
        val connections = mutableMapOf<XPeerID, XPeerConnection>()
    }

    private val chains: MutableMap<Long, Chain> = mutableMapOf<Long, Chain>()
    private val chainIDforBlockchainRID = mutableMapOf<ByteArrayKey, Long>()

    @Synchronized
    override fun connectChain(peerConfig: XChainPeerConfiguration, autoConnectAll: Boolean) {
        val chainID = peerConfig.chainID
        var ok = true
        if (chainID in chains) {
            disconnectChain(chainID)
            ok = false
        }
        chains[peerConfig.chainID] = Chain(peerConfig, autoConnectAll)
        chainIDforBlockchainRID[peerConfig.commConfiguration.blockchainRID.byteArrayKeyOf()] =
                peerConfig.chainID

        // TODO: auto connect all

        if (!ok) throw ProgrammerMistake("Error: multiple connections to for one chain")
    }

    @Synchronized
    override fun connectChainPeer(chainID: Long, peerID: XPeerID) {
        val chain = chains[chainID]
        if (chain == null) throw ProgrammerMistake("Chain ID not found")
        if (peerID !in chain.connections) { // ignore if already connected
            val peerInfo = chain.peerConfig.commConfiguration.resolvePeer(peerID.byteArray)!!
            connector.connectPeer(
                    XPeerConnectionDescriptor(
                            peerID,
                            chain.peerConfig.commConfiguration.blockchainRID.byteArrayKeyOf()
                    ),
                    peerInfo)
        }
    }

}