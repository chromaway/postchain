package net.postchain.network.netty

import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.network.PacketConverter
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XConnectorFactory

class NettyConnectorFactory<PC : PacketConverter<*>> : XConnectorFactory<PC> {
    override fun createConnector(myPeerInfo: PeerInfo,
                                 identPacketConverter: PC,
                                 eventReceiver: XConnectorEvents, cryptoSystem: CryptoSystem?) = NettyConnector(myPeerInfo, eventReceiver, identPacketConverter, cryptoSystem!!)
}