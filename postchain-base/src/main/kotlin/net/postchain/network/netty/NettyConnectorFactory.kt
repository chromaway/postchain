package net.postchain.network.netty

import net.postchain.base.PeerInfo
import net.postchain.network.IdentPacketConverter
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XConnectorFactory

class NettyConnectorFactory : XConnectorFactory {

    override fun createConnector(myPeerInfo: PeerInfo,
                                 identPacketConverter: IdentPacketConverter,
                                 eventReceiver: XConnectorEvents
    ) = NettyConnector(myPeerInfo, eventReceiver, identPacketConverter)
}