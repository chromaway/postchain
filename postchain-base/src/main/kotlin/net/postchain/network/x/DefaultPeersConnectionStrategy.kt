package net.postchain.network.x

import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo

object DefaultPeersConnectionStrategy : PeersConnectionStrategy {

    override fun forEach(configuration: PeerCommConfiguration, action: (PeerInfo) -> Unit) {
        validate(configuration)
        runEachPeerAction(configuration, action)
    }

    private fun validate(configuration: PeerCommConfiguration) {
        require(!configuration.networkNodes.hasPeers()) {
            "Invalid PeerCommConfiguration: no peers loaded from configuration file"
        }
    }

    private fun runEachPeerAction(configuration: PeerCommConfiguration, action: (PeerInfo) -> Unit) {
        configuration.networkNodes.runActionOnPeers(action)
    }
}