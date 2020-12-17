// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.netty2

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import net.postchain.base.PeerInfo
import net.postchain.core.Shutdownable
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPacketHandler

class IntTestContext(
        ownerPeerInfo: PeerInfo,
        peerInfos: Array<PeerInfo>
) : Shutdownable {

    var isShutdown = false

    val packets: XPacketHandler = mock()

    val events: XConnectorEvents = mock {
        on { onPeerConnected(any()) } doReturn packets
        on { onPeerDisconnected(any()) }.doAnswer { } // FYI: Instead of `doNothing` or `doReturn Unit`
    }

    val peer = NettyConnector<Int>(events)

    val packetEncoder = IntMockPacketEncoder(ownerPeerInfo)

    val packetDecoder = IntMockPacketDecoder(peerInfos)

    override fun shutdown() {
        if (!isShutdown) {
            peer.shutdown()
            isShutdown = true
        }
    }
}