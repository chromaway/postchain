package net.postchain.network.netty2

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import net.postchain.base.PeerCommConfiguration
import net.postchain.ebft.EbftPacketConverter
import net.postchain.ebft.message.EbftMessage
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPacketHandler

class EbftTestContext(config: PeerCommConfiguration) {

    val packets: XPacketHandler = mock()

    val events: XConnectorEvents = mock {
        on { onPeerConnected(any(), any()) } doReturn packets
    }

    val peer = NettyConnector(EbftPacketConverter(config), events)

    fun encodePacket(message: EbftMessage): ByteArray = peer.packetConverter.encodePacket(message)

    fun decodePacket(bytes: ByteArray): EbftMessage = peer.packetConverter.decodePacket(bytes)!!
}