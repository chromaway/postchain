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
        val myIndex = configuration.peerInfo.indexOfFirst { it.pubKey.contentEquals(configuration.pubKey) }
        val message = "Invalid PeerCommConfiguration: myIndex $myIndex " +
                "must be in range ${configuration.peerInfo.indices}"
        require(myIndex >= 0) { message }
        require(myIndex < configuration.peerInfo.size) { message }
    }

    private fun runEachPeerAction(configuration: PeerCommConfiguration, action: (PeerInfo) -> Unit) {
        val myIndex = configuration.peerInfo.indexOfFirst { it.pubKey.contentEquals(configuration.pubKey) }
        configuration.peerInfo
                // To avoid connection to itself.
                .filterIndexed { i, _ -> i != myIndex }
                // To avoid duplicating of connection between two peers since each peer has Server and Client entities.
                // TODO: Commented out for manager test scenario!
                // [et]: Has been uncommented again
                .filterIndexed { i, _ -> myIndex > i }
                .forEach(action)
    }
}