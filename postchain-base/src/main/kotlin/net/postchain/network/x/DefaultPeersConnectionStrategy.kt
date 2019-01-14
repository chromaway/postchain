package net.postchain.network.x

import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo

object DefaultPeersConnectionStrategy : PeersConnectionStrategy {

    override fun forEach(configuration: PeerCommConfiguration, action: (PeerInfo) -> Unit) {
        validate(configuration)
        runEachPeerAction(configuration, action)
    }

    private fun validate(configuration: PeerCommConfiguration) {

    }

    private fun runEachPeerAction(configuration: PeerCommConfiguration, action: (PeerInfo) -> Unit) {
        configuration.peerInfo
                // To avoid connection to itself.
                .filterIndexed { i, _ -> i != configuration.myIndex }
                // To avoid duplicating of connection between two peers since each peer has Server and Client entities.
                .filterIndexed { i, _ -> configuration.myIndex > i }
                .forEach(action)
    }
}