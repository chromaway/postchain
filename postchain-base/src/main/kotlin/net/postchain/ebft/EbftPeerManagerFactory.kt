package net.postchain.ebft

import net.postchain.base.PeerCommConfiguration
import net.postchain.ebft.message.EbftMessage
import net.postchain.network.PeerConnectionManager

@Deprecated("Deprecated after Netty2")
object EbftPeerManagerFactory {

    fun createConnectionManager(config: PeerCommConfiguration): PeerConnectionManager<EbftMessage> {
        return PeerConnectionManager(
                config.peerInfo[config.myIndex],
                EbftPacketConverter(config))
    }
}