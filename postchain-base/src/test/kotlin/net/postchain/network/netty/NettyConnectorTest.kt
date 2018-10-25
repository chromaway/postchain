package net.postchain.network.netty

import net.postchain.base.PeerID
import net.postchain.base.PeerInfo
import net.postchain.core.ByteArrayKey
import net.postchain.network.IdentPacketConverter
import net.postchain.network.IdentPacketInfo
import net.postchain.network.x.*
import org.awaitility.Awaitility
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

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

    inner class ServerConnectorEventsImplClose(private val receivedMessages: MutableList<String>,
                                          private val receivedErrors: MutableList<String>): XConnectorEvents {
        override fun onPeerConnected(descriptor: XPeerConnectionDescriptor, connection: XPeerConnection): XPacketHandler? {
            connections!!.add(connection)
            connection.close()
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

    @Test
    fun testNettyConnectorPositive() {
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

        Thread.sleep(2_000)
        connections!!.forEach {
            it.sendPacket { message.toByteArray() }
        }

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    Assert.assertEquals(listOf(message), serverReceivedMessages)
                    Assert.assertEquals(listOf(message), clientReceivedMessages)

                    Assert.assertTrue(serverReceivedErrors.isEmpty())
                    Assert.assertTrue(clientReceivedErrors.isEmpty())
                }
    }

    @Test
    fun testNettyConnectorMultiplePositive() {
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

        val client2ReceivedMessages = mutableListOf<String>()
        val client2ReceivedErrors = mutableListOf<String>()
        val peerInfo3 = PeerInfo("localhost", 8082, key.toByteArray())
        val connector3 = NettyConnector(peerInfo3, ClientConnectorEventsImpl(client2ReceivedMessages, client2ReceivedErrors), identPacketConverter)

        val xPeer2ConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId3".toByteArray()), ByteArrayKey("blockchainId3".toByteArray()))

        connector3.connectPeer(xPeer2ConnectionDescriptor, peerInfo2)

        Thread.sleep(2_000)
        connections!!.forEach {
            it.sendPacket { message.toByteArray() }
            it.sendPacket { message.toByteArray() }
        }
        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .untilAsserted {
                      Assert.assertEquals(listOf(message, message), serverReceivedMessages)
                      Assert.assertEquals(listOf(message, message), client2ReceivedMessages)
                      Assert.assertEquals(listOf(message, message, message, message), clientReceivedMessages)

                      Assert.assertTrue(serverReceivedErrors.isEmpty())
                      Assert.assertTrue(clientReceivedErrors.isEmpty())
                  }
    }

    @Test
    fun testNettyConnectorClosedConnection() {
        val identPacketConverter = IdentPacketConverterImpl()

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val peerInfo = PeerInfo("localhost", 8083, key.toByteArray())
        val connector = NettyConnector(peerInfo, ServerConnectorEventsImplClose(serverReceivedMessages, serverReceivedErrors), identPacketConverter)


        val clientReceivedMessages = mutableListOf<String>()
        val clientReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 8084, key.toByteArray())
        val connector2 = NettyConnector(peerInfo2, ClientConnectorEventsImpl(clientReceivedMessages, clientReceivedErrors), identPacketConverter)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId2".toByteArray()), ByteArrayKey("blockchainId2".toByteArray()))


        Thread.sleep(2_000)
        connections!!.forEach {
            it.sendPacket { message.toByteArray() }
        }
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    Assert.assertTrue(serverReceivedMessages.isEmpty())
                    Assert.assertTrue(clientReceivedMessages.isEmpty())

                    Assert.assertTrue(serverReceivedErrors.isEmpty())
                    Assert.assertTrue(clientReceivedErrors.isEmpty())
                }
    }

    @Test
    fun testNettyConnectorException() {
        val peerInfo = PeerInfo("localhost", 8089, key.toByteArray())

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val xConnectorEvents = ServerConnectorEventsImplException(serverReceivedMessages, serverReceivedErrors)

        val identPacketConverter = IdentPacketConverterImpl()

        val connector = NettyConnector(peerInfo, xConnectorEvents, identPacketConverter)

        val peerId = ByteArrayKey("peerId".toByteArray())
        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(peerId, ByteArrayKey("blockchainId".toByteArray()))

        connector.connectPeer(xPeerConnectionDescriptor, peerInfo)

        Awaitility.await()
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted {
                    Assert.assertTrue(serverReceivedErrors.contains(String(peerId.byteArray)))
                }
    }


    @Test
    fun testNettyPerformance() {
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

        Thread.sleep(2_000)
        val expectedServerReceivedMessages = mutableListOf<String>()
        val expectedClientReceivedMessages = mutableListOf<String>()
        (1 .. 1_000).forEach {
            connections!!.forEach {
                it.sendPacket { message.toByteArray() }
            }
            expectedServerReceivedMessages.add(message)
            expectedClientReceivedMessages.add(message)
        }

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    Assert.assertEquals(expectedServerReceivedMessages, serverReceivedMessages)
                    Assert.assertEquals(expectedClientReceivedMessages, clientReceivedMessages)

                    Assert.assertTrue(serverReceivedErrors.isEmpty())
                    Assert.assertTrue(clientReceivedErrors.isEmpty())
                }
    }
}