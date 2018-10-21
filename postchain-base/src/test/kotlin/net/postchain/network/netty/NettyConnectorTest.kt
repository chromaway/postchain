package net.postchain.network.netty

import net.postchain.base.PeerID
import net.postchain.base.PeerInfo
import net.postchain.core.ByteArrayKey
import net.postchain.network.IdentPacketConverter
import net.postchain.network.IdentPacketInfo
import net.postchain.network.x.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.RuntimeException

class NettyConnectorTest {

    private val key = "key"

    private val message = "msg"

    var connections: MutableList<XPeerConnection>? = null

    @Before
    fun setUp() {
        connections = mutableListOf()
    }

    inner class ServerConnectorEventsImpl(private val receivedMessages: MutableList<String>,
                                          private val receivedErrors: MutableList<String>): XConnectorEvents {
        override fun onPeerConnected(descriptor: XPeerConnectionDescriptor, connection: XPeerConnection): XPacketHandler? {
            connections!!.add(connection)
            return {
                data: ByteArray, peerID: XPeerID ->
                receivedMessages.add(String(data))
            }
        }

        override fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor) {
            receivedErrors.add(String(descriptor.peerID.byteArray))
        }
    }

    inner class ClientConnectorEventsImpl(private val receivedMessages: MutableList<String>,
                                          private val receivedErrors: MutableList<String>): XConnectorEvents {
        override fun onPeerConnected(descriptor: XPeerConnectionDescriptor, connection: XPeerConnection): XPacketHandler? {
            connections!!.add(connection)
            return {
                data: ByteArray, peerID: XPeerID ->
                receivedMessages.add(String(data))
            }
        }

        override fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor) {
            receivedErrors.add(String(descriptor.peerID.byteArray))
        }
    }

    inner class ServerConnectorEventsImplException(private val receivedMessages: MutableList<String>,
                                          private val receivedErrors: MutableList<String>): XConnectorEvents {
        override fun onPeerConnected(descriptor: XPeerConnectionDescriptor, connection: XPeerConnection): XPacketHandler? {
            throw RuntimeException()
        }

        override fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor) {
            receivedErrors.add(String(descriptor.peerID.byteArray))
        }
    }

    inner class IdentPacketConverterImpl: IdentPacketConverter {
        override fun makeIdentPacket(forPeer: PeerID) = forPeer

        override fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo  {
            return IdentPacketInfo(bytes, bytes)
        }

    }

    @Test(timeout = 10000)
    fun testNettyConnector() {
        val identPacketConverter = IdentPacketConverterImpl()

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val peerInfo = PeerInfo("localhost", 8080, key.toByteArray())
        val connector = NettyConnector(peerInfo, ServerConnectorEventsImpl(serverReceivedMessages, serverReceivedErrors), identPacketConverter)


        val clientReceivedMessages = mutableListOf<String>()
        val clientReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 8081, key.toByteArray())
        val connector2 = NettyConnector(peerInfo2, ClientConnectorEventsImpl(clientReceivedMessages, clientReceivedErrors), identPacketConverter)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId2".toByteArray()), ByteArrayKey("blockchainId2".toByteArray()))

        connector2.connectPeer(xPeerConnectionDescriptor, peerInfo)

        Thread.sleep(1_000)

        connections!!.forEach {
            it.sendPacket { message.toByteArray() }
        }
        while(!serverReceivedMessages.contains(message) && !clientReceivedMessages.contains(message)){}
        Assert.assertTrue(serverReceivedErrors.isEmpty())
        Assert.assertTrue(clientReceivedErrors.isEmpty())
    }

    @Test
    fun testNettyConnectorException() {
        val peerInfo = PeerInfo("localhost", 8080, key.toByteArray())

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val xConnectorEvents = ServerConnectorEventsImplException(serverReceivedMessages, serverReceivedErrors)

        val identPacketConverter = IdentPacketConverterImpl()

        val connector = NettyConnector(peerInfo, xConnectorEvents, identPacketConverter)

        val peerId = ByteArrayKey("peerId".toByteArray())
        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(peerId, ByteArrayKey("blockchainId".toByteArray()))

        connector.connectPeer(xPeerConnectionDescriptor, peerInfo)

        Thread.sleep(2_000)
        Assert.assertTrue(serverReceivedErrors.contains(String(peerId.byteArray)))
    }
}