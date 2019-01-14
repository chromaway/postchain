package net.postchain.network.netty2

import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.network.PacketConverter
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XConnectorFactory

class NettyConnectorFactory<PC : PacketConverter<*>> : XConnectorFactory<PC> {

    override fun createConnector(
            peerInfo: PeerInfo,
            packetConverter: PC,
            eventReceiver: XConnectorEvents,
            cryptoSystem: CryptoSystem?): XConnector {

        return NettyConnector(packetConverter, eventReceiver).apply {
            init(peerInfo)
        }
    }
}