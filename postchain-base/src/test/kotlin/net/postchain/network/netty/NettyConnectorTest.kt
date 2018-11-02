package net.postchain.network.netty

import net.postchain.base.PeerID
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.secp256k1_derivePubKey
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

    private val cryptoSystem = SECP256K1CryptoSystem()

    private val privateKey = "3132333435363738393031323334353637383930313233343536373839303131".toByteArray()
    private val privateKey2 = "3132333435363738393031323334353637383930313233343536373839303132".toByteArray()
    val publicKey = secp256k1_derivePubKey(privateKey)

    val publicKey2 = secp256k1_derivePubKey(privateKey2)

    private val message = "msg"

    var connections: MutableList<XPeerConnection>? = null

    @Before
    fun setUp() {
        connections = mutableListOf()
    }

    inner class ConnectorEventsImpl(private val receivedMessages: MutableList<String>,
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
        override fun makeIdentPacket(forPeer: PeerID): ByteArray {
            return forPeer
        }

        override fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo  {
            return IdentPacketInfo(bytes, bytes, bytes)
        }

    }

    @Test
    fun testNettyConnectorPositive() {

        val identPacketConverter = IdentPacketConverterImpl()

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val peerInfo = PeerInfo("localhost", 8081, publicKey, privateKey)
        val connector = NettyConnector(peerInfo, ConnectorEventsImpl(serverReceivedMessages, serverReceivedErrors), identPacketConverter, cryptoSystem)


        val clientReceivedMessages = mutableListOf<String>()
        val clientReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 8082, publicKey2, privateKey2)
        val connector2 = NettyConnector(peerInfo2, ConnectorEventsImpl(clientReceivedMessages, clientReceivedErrors), identPacketConverter, cryptoSystem)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId2".toByteArray()), ByteArrayKey("blockchainId2".toByteArray()))

        connector2.connectPeer(xPeerConnectionDescriptor, peerInfo)

        Thread.sleep(2_000)
        connections!!.forEach {
            it.sendPacket { message.toByteArray() }
        }
        Thread.sleep(2000)
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
        val peerInfo = PeerInfo("localhost", 8081, publicKey, "wrongKey".toByteArray())
        val connector = NettyConnector(peerInfo, ConnectorEventsImpl(serverReceivedMessages, serverReceivedErrors), identPacketConverter, cryptoSystem)


        val clientReceivedMessages = mutableListOf<String>()
        val clientReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 8082, publicKey2, privateKey2)
        val connector2 = NettyConnector(peerInfo2, ConnectorEventsImpl(clientReceivedMessages, clientReceivedErrors), identPacketConverter, cryptoSystem)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId2".toByteArray()), ByteArrayKey("blockchainId2".toByteArray()))

        connector2.connectPeer(xPeerConnectionDescriptor, peerInfo)

        Thread.sleep(2_000)
        connections!!.forEach {
            it.sendPacket { message.toByteArray() }
        }

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    Assert.assertTrue(clientReceivedErrors.size == 1)
                }
        connector.shutdown()
        connector2.shutdown()
    }

    // todo: fix, multiple encrypted sessions for passive connection
   // @Test
    fun testNettyConnectorMultiplePositive() {
        val identPacketConverter = IdentPacketConverterImpl()

        val peer1ReceivedMessages = mutableListOf<String>()
        val peer1ReceivedErrors = mutableListOf<String>()
        val peerInfo = PeerInfo("localhost", 8081, publicKey, privateKey)
        val connector = NettyConnector(peerInfo, ConnectorEventsImpl(peer1ReceivedMessages, peer1ReceivedErrors), identPacketConverter, cryptoSystem)


        val peer2ReceivedMessages = mutableListOf<String>()
        val peer2ReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 8082, publicKey, privateKey)
        val connector2 = NettyConnector(peerInfo2, ConnectorEventsImpl(peer2ReceivedMessages, peer2ReceivedErrors), identPacketConverter, cryptoSystem)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId2".toByteArray()), ByteArrayKey("blockchainId2".toByteArray()))

        connector2.connectPeer(xPeerConnectionDescriptor, peerInfo)

        val peer3ReceivedMessages = mutableListOf<String>()
        val peer3ReceivedErrors = mutableListOf<String>()
        val peerInfo3 = PeerInfo("localhost", 8083, publicKey, privateKey)
        val connector3 = NettyConnector(peerInfo3, ConnectorEventsImpl(peer3ReceivedMessages, peer3ReceivedErrors), identPacketConverter, cryptoSystem)

        val xPeer2ConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId3".toByteArray()), ByteArrayKey("blockchainId3".toByteArray()))

        connector3.connectPeer(xPeer2ConnectionDescriptor, peerInfo)

        Thread.sleep(8_000)
        connections!!.forEach {
            it.sendPacket { message.toByteArray() }
        }
        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .untilAsserted {
                      Assert.assertEquals(listOf(message, message), peer1ReceivedMessages)
                      Assert.assertEquals(listOf(message), peer2ReceivedMessages)
                      Assert.assertEquals(listOf(message), peer3ReceivedMessages)

                      Assert.assertTrue(peer1ReceivedErrors.isEmpty())
                      Assert.assertTrue(peer2ReceivedErrors.isEmpty())
                      Assert.assertTrue(peer3ReceivedErrors.isEmpty())
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
        val peerInfo = PeerInfo("localhost", 8083, publicKey, privateKey)
        val connector = NettyConnector(peerInfo, ConnectorEventsImpl(serverReceivedMessages, serverReceivedErrors), identPacketConverter,cryptoSystem)


        val clientReceivedMessages = mutableListOf<String>()
        val clientReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 8084, publicKey, privateKey)
        val connector2 = NettyConnector(peerInfo2, ConnectorEventsImpl(clientReceivedMessages, clientReceivedErrors), identPacketConverter, cryptoSystem)


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
        connector.shutdown()
        connector2.shutdown()
    }

    @Test
    fun testNettyConnectorException() {
        val peerInfo = PeerInfo("localhost", 8089, publicKey, privateKey)

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val xConnectorEvents = ServerConnectorEventsImplException(serverReceivedMessages, serverReceivedErrors)

        val identPacketConverter = IdentPacketConverterImpl()

        val connector = NettyConnector(peerInfo, xConnectorEvents, identPacketConverter, cryptoSystem)

        val peerId = ByteArrayKey("peerId".toByteArray())
        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(peerId, ByteArrayKey("blockchainId".toByteArray()))

        connector.connectPeer(xPeerConnectionDescriptor, peerInfo)
        Awaitility.await()
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted {
                    Assert.assertTrue(serverReceivedErrors.size == 1)
                }
        connector.shutdown()
    }


    @Test
    fun testNettyPerformance() {
        val identPacketConverter = IdentPacketConverterImpl()

        val serverReceivedMessages = mutableListOf<String>()
        val serverReceivedErrors = mutableListOf<String>()
        val peerInfo = PeerInfo("localhost", 8081, publicKey, privateKey)
        val connector = NettyConnector(peerInfo, ConnectorEventsImpl(serverReceivedMessages, serverReceivedErrors), identPacketConverter, cryptoSystem)


        val clientReceivedMessages = mutableListOf<String>()
        val clientReceivedErrors = mutableListOf<String>()
        val peerInfo2 = PeerInfo("localhost", 8082, publicKey, privateKey)
        val connector2 = NettyConnector(peerInfo2, ConnectorEventsImpl(clientReceivedMessages, clientReceivedErrors), identPacketConverter, cryptoSystem)

        val xPeerConnectionDescriptor = XPeerConnectionDescriptor(ByteArrayKey("peerId2".toByteArray()), ByteArrayKey("blockchainId2".toByteArray()))

        connector2.connectPeer(xPeerConnectionDescriptor, peerInfo)

        Thread.sleep(2_000)

        val requestAmount = 1_000
        //todo: ERROR ResourceLeakDetector - LEAK: ByteBuf.release() was not called before it's garbage-collected.
        val expectedServerReceivedMessages = (1 .. requestAmount).map { message }
        val expectedClientReceivedMessages = (1 .. requestAmount).map { message }
        (1 .. requestAmount).forEach {
            connections!!.forEach {
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