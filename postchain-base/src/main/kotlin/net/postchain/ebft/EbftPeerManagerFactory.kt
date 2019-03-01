package net.postchain.ebft

import net.postchain.base.PeerCommConfiguration
import net.postchain.ebft.message.Message
import net.postchain.network.PeerConnectionManager

object EbftPeerManagerFactory {

    fun createConnectionManager(config: PeerCommConfiguration): PeerConnectionManager<Message> {
        return PeerConnectionManager(
                config.peerInfo[config.myIndex],
                EbftPacketConverter(config))
    }
}