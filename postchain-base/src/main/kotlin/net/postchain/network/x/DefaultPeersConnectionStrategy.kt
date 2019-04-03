package net.postchain.network.x

import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo

object DefaultPeersConnectionStrategy : PeersConnectionStrategy {

    override fun forEach(configuration: PeerCommConfiguration, action: (PeerInfo) -> Unit) {
        validate(configuration)
        runEachPeerAction(configuration, action)
    }

    private fun validate(configuration: PeerCommConfiguration) {
        // peerInfo
        require(!configuration.peerInfo.isEmpty()) {
            "Invalid PeerCommConfiguration: peerInfo must not be empty"
        }

        // myIndex
        val message = "Invalid PeerCommConfiguration: myIndex ${configuration.myIndex} " +
                "must be in range ${configuration.peerInfo.indices}"
        require(configuration.myIndex >= 0) { message }
        require(configuration.myIndex < configuration.peerInfo.size) { message }
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