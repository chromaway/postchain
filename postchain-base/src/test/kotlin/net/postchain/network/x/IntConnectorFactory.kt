package net.postchain.network.x

import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.network.PacketConverter

class IntConnectorFactory : XConnectorFactory<PacketConverter<Int>> {

    override fun createConnector(
            myPeerInfo: PeerInfo,
            packetConverter: PacketConverter<Int>,
            eventReceiver: XConnectorEvents,
            cryptoSystem: CryptoSystem?): XConnector {

        return DefaultXConnector(
                myPeerInfo,
                packetConverter,
                eventReceiver)
    }
}