package net.postchain.network.netty

import net.postchain.base.PeerID
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.secp256k1_derivePubKey
import net.postchain.core.ByteArrayKey
import net.postchain.network.IdentPacketConverter
import net.postchain.network.x.*
import org.awaitility.Awaitility
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit

class NettyConnectorTest {

    private val cryptoSystem = SECP256K1CryptoSystem()

    private val privateKey = "3132333435363738393031323334353637383930313233343536373839303131".toByteArray()
    private val privateKey2 = "3132333435363738393031323334353637383930313233343536373839303132".toByteArray()
    val publicKey = secp256k1_derivePubKey(privateKey)

    val publicKey2 = secp256k1_derivePubKey(privateKey2)

    private val message = "msg"

    lateinit var connections: MutableList<XPeerConnection>

    @Before
    fun setUp() {
        connections = mutableListOf()
    }

    inner class ConnectorEventsImpl(private val receivedMessages: MutableList<String>,
                                    private val receivedErrors: MutableList<String>) : XConnectorEvents {
        override fun onPeerConnected(descriptor: XPeerConnectionDescriptor, connection: XPeerConnection): XPacketHandler? {
            synchronized(connections) {
                connections.add(connection)
            }
            return { data: ByteArray, peerID: XPeerID ->
                synchronized(receivedMessages) {
                    receivedMessages.add(String(data))
                }

            }
        }

        override fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor) {
            receivedErrors.add(String(descriptor.peerID.byteArray))
        }
    }

    inner class ServerConnectorEventsImplException(private val receivedMessages: MutableList<String>,
                                                   private val receivedErrors: MutableList<String>) : XConnectorEvents {
        override fun onPeerConnected(descriptor: XPeerConnectionDescriptor, connection: XPeerConnection): XPacketHandler? {
            throw RuntimeException()
        }

        override fun onPeerDisconnected(descriptor: XPeerConnectionDescriptor) {
            receivedErrors.add(String(descriptor.peerID.byteArray))
        }
    }

    inner class IdentPacketConverterImpl : IdentPacketConverter {
        override fun makeIdentPacket(forPeer: PeerID): ByteArray {
            return forPeer
        }

        override fun parseIdentPacket(bytes: ByteArray) = NettyIO.parseIdentPacket(bytes)

    }

    @Test
    fun testNettyConnectorPositive() {

        val identPacketConverter = IdentPacketConverterImpl()

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val peerInfo = PeerInfo("localhost", 9081, publicKey, privateKey)
        val connector = NettyConnector(peerInfo, ConnectorEventsImpl(serverReceivedMessages, serverReceivedErrors), identPacketConverter, cryptoSystem, false)


        val clientReceivedMessages = mutableListOf<String>()
        val clientReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 9082, publicKey2, privateKey2)
        val connector2 = NettyConnector(peerInfo2, ConnectorEventsImpl(clientReceivedMessages, clientReceivedErrors), identPacketConverter, cryptoSystem, false)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId2".toByteArray()), ByteArrayKey("blockchainId2".toByteArray()))

        connector2.connectPeer(xPeerConnectionDescriptor, peerInfo)

        Thread.sleep(2_000)
        connections.forEach {
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
        connector.shutdown()
        connector2.shutdown()
    }

    @Test
    fun testNettyConnectorWrongKey() {
        val identPacketConverter = IdentPacketConverterImpl()

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val peerInfo = PeerInfo("localhost", 9083, publicKey, "wrongKey".toByteArray())
        val connector = NettyConnector(peerInfo, ConnectorEventsImpl(serverReceivedMessages, serverReceivedErrors), identPacketConverter, cryptoSystem)


        val clientReceivedMessages = mutableListOf<String>()
        val clientReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 9084, publicKey2, privateKey2)
        val connector2 = NettyConnector(peerInfo2, ConnectorEventsImpl(clientReceivedMessages, clientReceivedErrors), identPacketConverter, cryptoSystem)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId2".toByteArray()), ByteArrayKey("blockchainId2".toByteArray()))

        connector2.connectPeer(xPeerConnectionDescriptor, peerInfo)

        Thread.sleep(2_000)
        connections.forEach {
            it.sendPacket { message.toByteArray() }
        }
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    Assert.assertTrue(serverReceivedErrors.size == 1)
                }
        connector.shutdown()
        connector2.shutdown()
    }

    @Test
    fun testNettyConnectorMultiplePositive() {
        val identPacketConverter = IdentPacketConverterImpl()

        val peer1ReceivedMessages = mutableListOf<String>()
        val peer1ReceivedErrors = mutableListOf<String>()
        val peerInfo = PeerInfo("localhost", 9085, publicKey, privateKey)
        val connector = NettyConnector(peerInfo, ConnectorEventsImpl(peer1ReceivedMessages, peer1ReceivedErrors), identPacketConverter, cryptoSystem)


        val peer2ReceivedMessages = mutableListOf<String>()
        val peer2ReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 9086, publicKey2, privateKey2)
        val connector2 = NettyConnector(peerInfo2, ConnectorEventsImpl(peer2ReceivedMessages, peer2ReceivedErrors), identPacketConverter, cryptoSystem)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId2".toByteArray()), ByteArrayKey("blockchainId2".toByteArray()))

        connector2.connectPeer(xPeerConnectionDescriptor, peerInfo)

        val peer3ReceivedMessages = mutableListOf<String>()
        val peer3ReceivedErrors = mutableListOf<String>()
        val peerInfo3 = PeerInfo("localhost", 9087, publicKey, privateKey)
        val connector3 = NettyConnector(peerInfo3, ConnectorEventsImpl(peer3ReceivedMessages, peer3ReceivedErrors), identPacketConverter, cryptoSystem)

        val xPeer2ConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId3".toByteArray()), ByteArrayKey("blockchainId3".toByteArray()))

        connector3.connectPeer(xPeer2ConnectionDescriptor, peerInfo)
        connector3.connectPeer(xPeer2ConnectionDescriptor, peerInfo2)


        val peer4ReceivedMessages = mutableListOf<String>()
        val peer4ReceivedErrors = mutableListOf<String>()
        val peerInfo4 = PeerInfo("localhost", 9088, publicKey, privateKey)
        val connector4 = NettyConnector(peerInfo4, ConnectorEventsImpl(peer4ReceivedMessages, peer4ReceivedErrors), identPacketConverter, cryptoSystem)

        val xPeer3ConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId4".toByteArray()), ByteArrayKey("blockchainId4".toByteArray()))

        connector4.connectPeer(xPeer3ConnectionDescriptor, peerInfo)
        connector4.connectPeer(xPeer3ConnectionDescriptor, peerInfo2)
        connector4.connectPeer(xPeer3ConnectionDescriptor, peerInfo3)

        Thread.sleep(2_000)

        connections.forEach {
            it.sendPacket { message.toByteArray() }
        }
        Thread.sleep(3_000)
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    Assert.assertEquals(listOf(message, message, message), peer1ReceivedMessages)
                    Assert.assertEquals(listOf(message, message, message), peer2ReceivedMessages)
                    Assert.assertEquals(listOf(message, message, message), peer3ReceivedMessages)
                    Assert.assertEquals(listOf(message, message, message), peer4ReceivedMessages)

                    Assert.assertTrue(peer1ReceivedErrors.isEmpty())
                    Assert.assertTrue(peer2ReceivedErrors.isEmpty())
                    Assert.assertTrue(peer3ReceivedErrors.isEmpty())
                    Assert.assertTrue(peer4ReceivedErrors.isEmpty())
                }
        connector.shutdown()
        connector2.shutdown()
        connector3.shutdown()
    }

    @Test
    fun testNettyConnectorClosedConnection() {
        val identPacketConverter = IdentPacketConverterImpl()

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val peerInfo = PeerInfo("localhost", 9088, publicKey, privateKey)
        val connector = NettyConnector(peerInfo, ConnectorEventsImpl(serverReceivedMessages, serverReceivedErrors), identPacketConverter, cryptoSystem)


        val clientReceivedMessages = mutableListOf<String>()
        val clientReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 9089, publicKey, privateKey)
        val connector2 = NettyConnector(peerInfo2, ConnectorEventsImpl(clientReceivedMessages, clientReceivedErrors), identPacketConverter, cryptoSystem)


        Thread.sleep(2_000)
        connections.forEach {
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
        connector.shutdown()
        connector2.shutdown()
    }

    @Test
    fun testNettyConnectorException() {
        val peerInfo = PeerInfo("localhost", 9090, publicKey, privateKey)

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val xConnectorEvents = ServerConnectorEventsImplException(serverReceivedMessages, serverReceivedErrors)

        val identPacketConverter = IdentPacketConverterImpl()

        val connector = NettyConnector(peerInfo, xConnectorEvents, identPacketConverter, cryptoSystem)

        val peerId = ByteArrayKey("peerId".toByteArray())
        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(peerId, ByteArrayKey("blockchainId".toByteArray()))

        connector.connectPeer(xPeerConnectionDescriptor, peerInfo)
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    Assert.assertTrue(serverReceivedErrors.size == 1)
                }
        connector.shutdown()
    }

    @Test
    @Ignore("Due to java.util.ConcurrentModificationException in some cases")
    fun testNettyPerformance() {
        val identPacketConverter = IdentPacketConverterImpl()

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val peerInfo = PeerInfo("localhost", 9091, publicKey, privateKey)
        val connector = NettyConnector(peerInfo, ConnectorEventsImpl(serverReceivedMessages, serverReceivedErrors), identPacketConverter, cryptoSystem)


        val clientReceivedMessages = mutableListOf<String>()
        val clientReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 9092, publicKey, privateKey)
        val connector2 = NettyConnector(peerInfo2, ConnectorEventsImpl(clientReceivedMessages, clientReceivedErrors), identPacketConverter, cryptoSystem)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId2".toByteArray()), ByteArrayKey("blockchainId2".toByteArray()))

        connector2.connectPeer(xPeerConnectionDescriptor, peerInfo)

        Thread.sleep(2_000)

        val requestAmount = 100_000
        val expectedServerReceivedMessages = (1..requestAmount).map { message }
        val expectedClientReceivedMessages = expectedServerReceivedMessages
        (1..requestAmount).forEach {
            connections.forEach {
                it.sendPacket { message.toByteArray() }
            }
        }
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    Assert.assertEquals(expectedServerReceivedMessages, serverReceivedMessages)
                    Assert.assertEquals(expectedClientReceivedMessages, clientReceivedMessages)

                    Assert.assertTrue(serverReceivedErrors.isEmpty())
                    Assert.assertTrue(clientReceivedErrors.isEmpty())
                }
        connector.shutdown()
        connector2.shutdown()
    }
}