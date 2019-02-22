package net.postchain.network.netty

import net.postchain.base.CryptoSystem
import net.postchain.base.PeerInfo
import net.postchain.network.PacketConverter
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XConnectorFactory

// TODO: Remove it. For temporarily purpose only.
interface XConnectorFactoryOld<PC : PacketConverter<*>> {
    fun createConnector(myPeerInfo: PeerInfo,
                        packetConverter: PC,
                        eventReceiver: XConnectorEvents,
                        cryptoSystem: CryptoSystem? = null): XConnectorOld
}

class NettyConnectorFactory<PC : PacketConverter<*>> : XConnectorFactoryOld<PC> {
    override fun createConnector(myPeerInfo: PeerInfo,
                                 identPacketConverter: PC,
                                 eventReceiver: XConnectorEvents, cryptoSystem: CryptoSystem?) = NettyConnector(myPeerInfo, eventReceiver, identPacketConverter, cryptoSystem!!)
}