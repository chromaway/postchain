// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.netty2

import assertk.assert
import assertk.assertions.isIn
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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
import org.junit.After
import org.junit.Before
import org.junit.Test

class IntNettyConnector3PeersCommunicationIT {

    private val blockchainRid = BlockchainRid.buildRepeat(0x01)
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
    fun testConnectAndCommunicate() {
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

        // Sending packets
        // * 1 -> 2 and 1 -> 3
        val packet1 = byteArrayOf(10, 2, 3, 4)
        connection1.firstValue.sendPacket { packet1 }
        connection1.secondValue.sendPacket { packet1 }
        // * 2 -> 1 and 2 -> 3
        val packet2 = byteArrayOf(1, 20, 3, 4)
        connection2.firstValue.sendPacket { packet2 }
        connection2.secondValue.sendPacket { packet2 }
        // * 3 -> 1 and 3 -> 2
        val packet3 = byteArrayOf(1, 2, 30, 4)
        connection3.firstValue.sendPacket { packet3 }
        connection3.secondValue.sendPacket { packet3 }

        // * asserting
        await().atMost(TEN_SECONDS)
                .untilAsserted {
                    // Peer1
                    val packets1 = argumentCaptor<ByteArray>()
                    val expected1 = arrayOf(packet2, packet3).map(ByteArray::byteArrayKeyOf).toTypedArray()
                    verify(context1.packets, times(2)).invoke(packets1.capture(), any())
                    assert(packets1.firstValue.byteArrayKeyOf()).isIn(*expected1)
                    assert(packets1.secondValue.byteArrayKeyOf()).isIn(*expected1)

                    // Peer2
                    val packets2 = argumentCaptor<ByteArray>()
                    val expected2 = arrayOf(packet1, packet3).map(ByteArray::byteArrayKeyOf).toTypedArray()
                    verify(context2.packets, times(2)).invoke(packets2.capture(), any())
                    assert(packets2.firstValue.byteArrayKeyOf()).isIn(*expected2)
                    assert(packets2.secondValue.byteArrayKeyOf()).isIn(*expected2)

                    // Peer3
                    val packets3 = argumentCaptor<ByteArray>()
                    val expected3 = arrayOf(packet1, packet2).map(ByteArray::byteArrayKeyOf).toTypedArray()
                    verify(context3.packets, times(2)).invoke(packets3.capture(), any())
                    assert(packets3.firstValue.byteArrayKeyOf()).isIn(*expected3)
                    assert(packets3.secondValue.byteArrayKeyOf()).isIn(*expected3)
                }
    }

}