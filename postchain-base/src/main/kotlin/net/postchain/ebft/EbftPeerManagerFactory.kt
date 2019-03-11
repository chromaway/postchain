package net.postchain.ebft

import net.postchain.base.PeerCommConfiguration
import net.postchain.ebft.message.EbftMessage
import net.postchain.network.PeerConnectionManager

object EbftPeerManagerFactory {

    fun createConnectionManager(config: PeerCommConfiguration): PeerConnectionManager<EbftMessage> {
        return PeerConnectionManager(
                config.myPeerInfo(),
                EbftPacketConverter(config))
    }
}