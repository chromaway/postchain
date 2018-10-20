package net.postchain.network.netty

import net.postchain.base.PeerID
import net.postchain.base.PeerInfo
import net.postchain.core.ByteArrayKey
import net.postchain.network.IdentPacketConverter
import net.postchain.network.IdentPacketInfo
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnection
import net.postchain.network.x.XPeerConnectionDescriptor
import org.junit.Test

class NettyConnectorTest {

    inner class ConnectorEventsImpl: XConnectorEvents {
        override fun onPeerConnected(descriptor: XPeerConnectionDescriptor, connection: XPeerConnection) {
            connection.sendPacket { "Hi there".toByteArray() }
            println("peer connected")
        }

        override fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor) {
            println("peer disconnected")
        }

    }

    inner class IdentPacketConverterImpl: IdentPacketConverter {
        override fun makeIdentPacket(forPeer: PeerID) = forPeer

        override fun parseIdentPacket(bytes: ByteArray) = IdentPacketInfo(bytes, bytes)

    }

    @Test
    fun testNettyConnector() {
        val peerInfo = PeerInfo("localhost", 8080, "key".toByteArray())
        val xConnectorEvents = ConnectorEventsImpl()
        val identPacketConverter = IdentPacketConverterImpl()

        val connector = NettyConnector(peerInfo, xConnectorEvents, identPacketConverter)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId".toByteArray()), ByteArrayKey("peerId".toByteArray()))

        connector.connectPeer(xPeerConnectionDescriptor, peerInfo)

        Thread.sleep(10_000)
    }
}