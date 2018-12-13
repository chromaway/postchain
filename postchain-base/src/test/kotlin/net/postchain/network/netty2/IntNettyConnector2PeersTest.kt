package net.postchain.network.netty2

import assertk.assert
import assertk.assertions.isIn
import assertk.isContentEqualTo
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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

class IntNettyConnector2PeersTest {

    private val blockchainRid = byteArrayOf(0x01)
    private lateinit var peerInfo1: PeerInfo
    private lateinit var peerInfo2: PeerInfo
    private lateinit var context1: IntTestContext
    private lateinit var context2: IntTestContext

    @Before
    fun setUp() {
        peerInfo1 = PeerInfo("localhost", 3331, byteArrayOf(0, 0, 0, 1))
        peerInfo2 = PeerInfo("localhost", 3332, byteArrayOf(0, 0, 0, 2))

        // Creating
        context1 = IntTestContext(peerInfo1, arrayOf(peerInfo1, peerInfo2))
        context2 = IntTestContext(peerInfo2, arrayOf(peerInfo1, peerInfo2))

        // Initializing
        context1.peer.init(peerInfo1)
        context2.peer.init(peerInfo2)
    }

    @After
    fun tearDown() {
        context1.peer.shutdown()
        context2.peer.shutdown()
    }

    @Test
    fun twoPeers_ConnectAndCommunicate_Successfully() {
        // Connecting 1 -> 2
        val peerDescriptor2 = XPeerConnectionDescriptor(peerInfo2.peerId(), blockchainRid.byteArrayKeyOf())
        context1.peer.connectPeer(peerDescriptor2, peerInfo2)

        // Waiting for all connections establishing
        val (descriptor1, connection1) = argumentCaptor2<XPeerConnectionDescriptor, XPeerConnection>()
        val (descriptor2, connection2) = argumentCaptor2<XPeerConnectionDescriptor, XPeerConnection>()
        await().atMost(FIVE_SECONDS)
                .untilAsserted {
                    verify(context1.events).onPeerConnected(descriptor1.capture(), connection1.capture())
                    assert(descriptor1.firstValue.peerId.byteArray).isContentEqualTo(peerInfo2.pubKey)

                    verify(context2.events).onPeerConnected(descriptor2.capture(), connection2.capture())
                    assert(descriptor2.firstValue.peerId.byteArray).isContentEqualTo(peerInfo1.pubKey)
                }

        // Sending packets
        // * 1 -> 2
        val packets1 = arrayOf(
                byteArrayOf(1, 2, 3, 4),
                byteArrayOf(10, 2, 3, 4),
                byteArrayOf(100, 2, 3, 4))
        connection1.firstValue.sendPacket { packets1[0] }
        connection1.firstValue.sendPacket { packets1[1] }
        connection1.firstValue.sendPacket { packets1[2] }
        // * 2 -> 1
        val packets2 = arrayOf(
                byteArrayOf(1, 2, 3, 4),
                byteArrayOf(10, 20, 30, 40))
        connection2.firstValue.sendPacket { packets2[0] }
        connection2.firstValue.sendPacket { packets2[1] }

        // * asserting
        await().atMost(TEN_SECONDS)
                .untilAsserted {
                    // Peer1
                    val actualPackets1 = argumentCaptor<ByteArray>()
                    val expected1 = packets2.map(ByteArray::byteArrayKeyOf).toTypedArray()
                    verify(context1.packets, times(2)).invoke(actualPackets1.capture(), any())
                    assert(actualPackets1.firstValue.byteArrayKeyOf()).isIn(*expected1)
                    assert(actualPackets1.secondValue.byteArrayKeyOf()).isIn(*expected1)

                    // Peer2
                    val actualPackets2 = argumentCaptor<ByteArray>()
                    val expected2 = packets1.map(ByteArray::byteArrayKeyOf).toTypedArray()
                    verify(context2.packets, times(3)).invoke(actualPackets2.capture(), any())
                    assert(actualPackets2.firstValue.byteArrayKeyOf()).isIn(*expected2)
                    assert(actualPackets2.secondValue.byteArrayKeyOf()).isIn(*expected2)
                    assert(actualPackets2.thirdValue.byteArrayKeyOf()).isIn(*expected2)
                }
    }

}