package net.postchain.network.netty2

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import net.postchain.base.PeerInfo
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPacketHandler

class IntTestContext(
        ownerPeerInfo: PeerInfo,
        peerInfos: Array<PeerInfo>
) {

    val packets: XPacketHandler = mock()

    val events: XConnectorEvents = mock {
        on { onPeerConnected(any(), any()) } doReturn packets
    }

    val peer = NettyConnector<Int>(
//            IntMockIntegerPacketConverter(ownerPeerInfo, peerInfos),
            events)

    val packetEncoder = IntMockPacketEncoder(ownerPeerInfo)

    val packetDecoder = IntMockPacketDecoder(peerInfos)
}