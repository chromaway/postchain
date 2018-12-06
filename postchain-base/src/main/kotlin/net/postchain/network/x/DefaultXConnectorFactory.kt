package net.postchain.network.x

import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.ebft.EbftPacketConverter

class DefaultXConnectorFactory : XConnectorFactory<EbftPacketConverter> {

    override fun createConnector(
            myPeerInfo: PeerInfo,
            packetConverter: EbftPacketConverter,
            eventReceiver: XConnectorEvents,
            cryptoSystem: CryptoSystem?): XConnector {

        return DefaultXConnector(
                myPeerInfo,
                packetConverter,
                eventReceiver)
    }
}