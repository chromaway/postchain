// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.netty2

import net.postchain.base.PeerInfo
import net.postchain.network.XPacketDecoder
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XConnectorFactory

class NettyConnectorFactory<PacketType> : XConnectorFactory<PacketType> {

    override fun createConnector(
            peerInfo: PeerInfo,
            packetDecoder: XPacketDecoder<PacketType>,
            eventReceiver: XConnectorEvents
    ): XConnector<PacketType> {

        return NettyConnector<PacketType>(eventReceiver).apply {
            init(peerInfo, packetDecoder)
        }
    }
}