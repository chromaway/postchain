package net.postchain.network

import net.postchain.core.ProgrammerMistake

abstract class XAbstractConnectionManager: XConnectionManager {

    class Connection(
            val peerID: XPeerID
    )

    class Chain(
            val peerConfig: XChainPeerConfiguration,
            val connectAll: Boolean,
            val connections: List<Connection>
    )

    val chains: MutableMap<Long, Chain> = mutableMapOf<Long, Chain>()

    override fun connectChain(peerConfig: XChainPeerConfiguration, autoConnectAll: Boolean) {
        val chainID = peerConfig.chainID
        var ok = true
        if (chainID in chains) {
            disconnectChain(chainID)
            ok = false
        }
        chains[peerConfig.chainID] = Chain(peerConfig, autoConnectAll, mutableListOf())
        if (!ok) throw ProgrammerMistake("Error: multiple connections to for one chain")
    }

}