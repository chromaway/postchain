// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.common.toHex

object DefaultPeersConnectionStrategy : PeersConnectionStrategy {

    override fun forEach(configuration: PeerCommConfiguration, action: (PeerInfo) -> Unit) {
        validate(configuration)
        runEachPeerAction(configuration, action)
    }

    private fun validate(configuration: PeerCommConfiguration) {
        // We are allowed to go on with only one peer, this node itself, and it will not be in the peer list
        /*require(configuration.networkNodes.hasPeers()) {
            "Invalid PeerCommConfiguration: no peers loaded from configuration file"
        } */
    }

    /**
     * This is the implementation this strategy uses for how to decide what peer should interract (usually connect)
     * with other peers.
     *
     * Here we use the string version of the public key of the peer to make sure no double connects are done.
     */
    fun getPeersThatShouldDoAction(peerMap: Map<XPeerID, PeerInfo>, myKey: XPeerID): Set<PeerInfo> {
        val myKeyAsString = myKey.byteArray.toHex()
        val keysAsStringMap: Map<String, PeerInfo> = peerMap.map{ it.key.byteArray.toHex() to it.value }.toMap()

        return keysAsStringMap.filter { myKeyAsString.compareTo(it.key) > 0 }.values.toSet()
    }

    private fun runEachPeerAction(configuration: PeerCommConfiguration, action: (PeerInfo) -> Unit) {
        configuration.networkNodes.filterAndRunActionOnPeers(DefaultPeersConnectionStrategy::getPeersThatShouldDoAction, action)
    }
}