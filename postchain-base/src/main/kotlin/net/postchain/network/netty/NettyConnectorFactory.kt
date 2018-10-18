package net.postchain.network.netty

import net.postchain.base.PeerInfo
import net.postchain.network.*

class NettyConnectorFactory: XConnectorFactory {
    override fun createConnector(myPeerInfo: PeerInfo, eventReceiver: XConnectorEvents, identPacketConverter: IdentPacketConverter): XConnector {
        return NettyConnector(eventReceiver, identPacketConverter)
    }
}