// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.netty2

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isIn
import com.nhaarman.mockitokotlin2.*
import net.postchain.base.BlockchainRid
import net.postchain.base.PeerInfo
import net.postchain.base.peerId
import net.postchain.core.byteArrayKeyOf
import net.postchain.devtools.argumentCaptor2
import net.postchain.network.x.XPeerConnection
import net.postchain.network.x.XPeerConnectionDescriptor
import org.awaitility.Awaitility.await
import org.awaitility.Duration.FIVE_SECONDS
import org.awaitility.Duration.TEN_SECONDS
import org.awaitility.kotlin.withPollDelay
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Based on [IntNettyConnector3PeersCommunicationIT]
 */
class IntNettyConnector3PeersReconnectionIT {

    private val blockchainRid = BlockchainRid(byteArrayOf(0x01))
    private lateinit var peerInfo1: PeerInfo
    private lateinit var peerInfo2: PeerInfo
    private lateinit var peerInfo3: PeerInfo
    private lateinit var context1: IntTestContext
    private lateinit var context2: IntTestContext
    private lateinit var context3: IntTestContext

    @Before
    fun setUp() {
        peerInfo1 = PeerInfo("localhost", 3331, byteArrayOf(0, 0, 0, 1))
        peerInfo2 = PeerInfo("localhost", 3332, byteArrayOf(0, 0, 0, 2))
        peerInfo3 = PeerInfo("localhost", 3333, byteArrayOf(0, 0, 0, 3))

        // Starting contexts
        context1 = startContext(peerInfo1)
        context2 = startContext(peerInfo2)
        context3 = startContext(peerInfo3)
    }

    @After
    fun tearDown() {
        stopContext(context1)
        stopContext(context2)
        stopContext(context3)
    }

    private fun startContext(peerInfo: PeerInfo): IntTestContext {
        return IntTestContext(peerInfo, arrayOf(peerInfo1, peerInfo2, peerInfo3))
                .also {
                    it.peer.init(peerInfo, it.packetDecoder)
                }
    }

    private fun stopContext(context: IntTestContext) {
        context.shutdown()
    }

    @Test
    fun testConnectDisconnectAndConnectAgain() {
        // Connecting
        // * 1 -> 2
        val peerDescriptor2 = XPeerConnectionDescriptor(peerInfo2.peerId(), blockchainRid)
        context1.peer.connectPeer(peerDescriptor2, peerInfo2, context1.packetEncoder)
        // * 1 -> 3
        val peerDescriptor3 = XPeerConnectionDescriptor(peerInfo3.peerId(), blockchainRid)
        context1.peer.connectPeer(peerDescriptor3, peerInfo3, context1.packetEncoder)
        // * 3 -> 2
        context3.peer.connectPeer(peerDescriptor2, peerInfo2, context3.packetEncoder)

        // Waiting for all connections to be established
        val connection1 = argumentCaptor<XPeerConnection>()
        val connection2 = argumentCaptor<XPeerConnection>()
        val connection3 = argumentCaptor<XPeerConnection>()
        await().atMost(FIVE_SECONDS)
                .untilAsserted {
                    // 1
                    val expected1 = arrayOf(peerInfo2, peerInfo3).map(PeerInfo::peerId).toTypedArray()
                    verify(context1.events, times(2)).onPeerConnected(connection1.capture())
                    assert(connection1.firstValue.descriptor().peerId).isIn(*expected1)
                    assert(connection1.secondValue.descriptor().peerId).isIn(*expected1)

                    // 2
                    val expected2 = arrayOf(peerInfo1, peerInfo3).map(PeerInfo::peerId).toTypedArray()
                    verify(context2.events, times(2)).onPeerConnected(connection2.capture())
                    assert(connection2.firstValue.descriptor().peerId).isIn(*expected2)
                    assert(connection2.secondValue.descriptor().peerId).isIn(*expected2)

                    // 3
                    val expected3 = arrayOf(peerInfo1, peerInfo2).map(PeerInfo::peerId).toTypedArray()
                    verify(context3.events, times(2)).onPeerConnected(connection3.capture())
                    assert(connection3.firstValue.descriptor().peerId).isIn(*expected3)
                    assert(connection3.secondValue.descriptor().peerId).isIn(*expected3)
                }

        // Disconnecting: peer3 disconnects from peer1 and peer2
        stopContext(context3)

        val connectionCapture1 = argumentCaptor<XPeerConnection>()
        val disconnectPeerDescriptor2 = argumentCaptor<XPeerConnectionDescriptor>()
        await().atMost(TEN_SECONDS)
                .untilAsserted {
                    // Asserting peer3 is disconnected from peer1
                    verify(context1.events, times(1))
                            .onPeerDisconnected(connectionCapture1.capture())
                    assert(connectionCapture1.firstValue.descriptor().peerId).isEqualTo(peerInfo3.peerId())

                    // Asserting peer3 is disconnected from peer2
                    // never() -- because of peer2 is a server for peer3
                    verify(context2.events, never()).onPeerDisconnected(any())
                }

        // Sending packets
        // * 1 -> 2 and 1 -> 3
        val packet1 = byteArrayOf(10, 2, 3, 4)
        connection1.firstValue.sendPacket { packet1 }
        connection1.secondValue.sendPacket { packet1 }

        // Asserting peer2 have received packet1
        await().atMost(FIVE_SECONDS)
                .untilAsserted {
                    // Peer2
                    val packets2 = argumentCaptor<ByteArray>()
                    verify(context2.packets, times(1)).invoke(packets2.capture(), any())
                    assert(packets2.firstValue.byteArrayKeyOf()).isEqualTo(packet1.byteArrayKeyOf())
                }

        // Asserting peer3 haven't received packet1
        await().withPollDelay(FIVE_SECONDS)
                .atMost(FIVE_SECONDS.multiply(2))
                .untilAsserted {
                    // Peer3
                    verify(context3.packets, never()).invoke(any(), any())
                }

        // Re-borning of peer3
        context3 = startContext(peerInfo3)

        // Re-connecting
        // * 3 -> 1
        val peerDescriptor1 = XPeerConnectionDescriptor(peerInfo1.peerId(), blockchainRid)
        context3.peer.connectPeer(peerDescriptor1, peerInfo1, context3.packetEncoder)
        // * 3 -> 2
        context3.peer.connectPeer(peerDescriptor2, peerInfo2, context3.packetEncoder)

        // Waiting for all connections to be established
        val connection1_2 = argumentCaptor<XPeerConnection>()
        val connection2_2 = argumentCaptor<XPeerConnection>()
        val connection3_2 = argumentCaptor<XPeerConnection>()
        await().atMost(FIVE_SECONDS)
                .untilAsserted {
                    // 1
                    verify(context1.events, times(3)).onPeerConnected(connection1_2.capture())
                    assert(connection1_2.thirdValue.descriptor().peerId).isEqualTo(peerInfo3.peerId())

                    // 2
                    verify(context2.events, times(3)).onPeerConnected(connection2_2.capture())
                    assert(connection2_2.thirdValue.descriptor().peerId).isEqualTo(peerInfo3.peerId())

                    // 3
                    val expected3 = arrayOf(peerInfo1, peerInfo2).map(PeerInfo::peerId).toTypedArray()
                    verify(context3.events, times(2)).onPeerConnected(connection3_2.capture())
                    assert(connection3_2.firstValue.descriptor().peerId).isIn(*expected3)
                    assert(connection3_2.secondValue.descriptor().peerId).isIn(*expected3)
                }

        // Sending packets
        // * 1 -> 3
        val packet1_2 = byteArrayOf(10, 2, 3, 4)
        connection1_2.thirdValue.sendPacket { packet1_2 }
        // * 2 -> 3
        val packet2_2 = byteArrayOf(1, 20, 3, 4)
        connection2_2.thirdValue.sendPacket { packet2_2 }
        // * 3 -> 1 and 3 -> 2
        val packet3_2 = byteArrayOf(1, 2, 30, 4)
        connection3_2.firstValue.sendPacket { packet3_2 }
        connection3_2.secondValue.sendPacket { packet3_2 }

        // * asserting
        await().atMost(TEN_SECONDS)
                .untilAsserted {
                    // Peer1
                    val packets1 = argumentCaptor<ByteArray>()
                    verify(context1.packets, times(1)).invoke(packets1.capture(), any())
                    assert(packets1.firstValue.byteArrayKeyOf()).isEqualTo(packet3_2.byteArrayKeyOf())

                    // Peer2
                    val packets2 = argumentCaptor<ByteArray>()
                    val expected2 = arrayOf(packet1, packet3_2).map(ByteArray::byteArrayKeyOf).toTypedArray()
                    verify(context2.packets, times(2)).invoke(packets2.capture(), any())
                    assert(packets2.firstValue.byteArrayKeyOf()).isIn(*expected2)
                    assert(packets2.secondValue.byteArrayKeyOf()).isIn(*expected2)

                    // Peer3
                    val packets3 = argumentCaptor<ByteArray>()
                    val expected3 = arrayOf(packet1_2, packet2_2).map(ByteArray::byteArrayKeyOf).toTypedArray()
                    verify(context3.packets, times(2)).invoke(packets3.capture(), any())
                    assert(packets3.firstValue.byteArrayKeyOf()).isIn(*expected3)
                    assert(packets3.secondValue.byteArrayKeyOf()).isIn(*expected3)
                }
    }

}