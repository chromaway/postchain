package net.postchain.network.netty2

import assertk.assert
import assertk.assertions.isIn
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import net.postchain.base.*
import net.postchain.core.byteArrayKeyOf
import net.postchain.devtools.argumentCaptor2
import net.postchain.ebft.message.GetBlockAtHeight
import net.postchain.network.x.XPeerConnection
import net.postchain.network.x.XPeerConnectionDescriptor
import org.awaitility.Awaitility.await
import org.awaitility.Duration.FIVE_SECONDS
import org.awaitility.Duration.TEN_SECONDS
import org.junit.After
import org.junit.Before
import org.junit.Test

class EbftNettyConnector3PeersTest {

    private val cryptoSystem = SECP256K1CryptoSystem()
    private val blockchainRid = ByteArray(64, Int::toByte)

    private lateinit var peerInfo1: PeerInfo
    private lateinit var peerInfo2: PeerInfo
    private lateinit var peerInfo3: PeerInfo

    private lateinit var context1: EbftTestContext
    private lateinit var context2: EbftTestContext
    private lateinit var context3: EbftTestContext

    @Before
    fun setUp() {
        val privKey1 = cryptoSystem.getRandomBytes(32)
        val pubKey1 = secp256k1_derivePubKey(privKey1)

        val privKey2 = cryptoSystem.getRandomBytes(32)
        val pubKey2 = secp256k1_derivePubKey(privKey2)

        val privKey3 = cryptoSystem.getRandomBytes(32)
        val pubKey3 = secp256k1_derivePubKey(privKey3)

        peerInfo1 = PeerInfo("localhost", 3331, pubKey1)
        peerInfo2 = PeerInfo("localhost", 3332, pubKey2)
        peerInfo3 = PeerInfo("localhost", 3333, pubKey3)
        val peers = arrayOf(peerInfo1, peerInfo2, peerInfo3)

        // Creating
        context1 = EbftTestContext(
                BasePeerCommConfiguration(peers, blockchainRid, 0, cryptoSystem, privKey1))
        context2 = EbftTestContext(
                BasePeerCommConfiguration(peers, blockchainRid, 1, cryptoSystem, privKey2))
        context3 = EbftTestContext(
                BasePeerCommConfiguration(peers, blockchainRid, 2, cryptoSystem, privKey3))

        // Initializing
        context1.peer.init(peerInfo1)
        context2.peer.init(peerInfo2)
        context3.peer.init(peerInfo3)
    }

    @After
    fun tearDown() {
        context1.peer.shutdown()
        context2.peer.shutdown()
        context3.peer.shutdown()
    }

    @Test
    fun threePeers_ConnectAndCommunicate_Successfully() {
        // Connecting
        // * 1 -> 2
        val peerDescriptor2 = XPeerConnectionDescriptor(peerInfo2.peerId(), blockchainRid.byteArrayKeyOf())
        context1.peer.connectPeer(peerDescriptor2, peerInfo2)
        // * 1 -> 3
        val peerDescriptor3 = XPeerConnectionDescriptor(peerInfo3.peerId(), blockchainRid.byteArrayKeyOf())
        context1.peer.connectPeer(peerDescriptor3, peerInfo3)
        // * 3 -> 2
        context3.peer.connectPeer(peerDescriptor2, peerInfo2)

        // Waiting for all connections establishing
        val (descriptor1, connection1) = argumentCaptor2<XPeerConnectionDescriptor, XPeerConnection>()
        val (descriptor2, connection2) = argumentCaptor2<XPeerConnectionDescriptor, XPeerConnection>()
        val (descriptor3, connection3) = argumentCaptor2<XPeerConnectionDescriptor, XPeerConnection>()
        await().atMost(FIVE_SECONDS)
                .untilAsserted {
                    // 1
                    val expected1 = arrayOf(peerInfo2, peerInfo3).map(PeerInfo::peerId).toTypedArray()
                    verify(context1.events, times(2)).onPeerConnected(descriptor1.capture(), connection1.capture())
                    assert(descriptor1.firstValue.peerId).isIn(*expected1)
                    assert(descriptor1.secondValue.peerId).isIn(*expected1)

                    // 2
                    val expected2 = arrayOf(peerInfo1, peerInfo3).map(PeerInfo::peerId).toTypedArray()
                    verify(context2.events, times(2)).onPeerConnected(descriptor2.capture(), connection2.capture())
                    assert(descriptor2.firstValue.peerId).isIn(*expected2)
                    assert(descriptor2.secondValue.peerId).isIn(*expected2)

                    // 3
                    val expected3 = arrayOf(peerInfo1, peerInfo2).map(PeerInfo::peerId).toTypedArray()
                    verify(context3.events, times(2)).onPeerConnected(descriptor3.capture(), connection3.capture())
                    assert(descriptor3.firstValue.peerId).isIn(*expected3)
                    assert(descriptor3.secondValue.peerId).isIn(*expected3)
                }

        // Sending packets
        // * 1 -> 2
        val packets1 = arrayOf(
                GetBlockAtHeight(10),
                GetBlockAtHeight(11))
        connection1.firstValue.sendPacket { context1.encodePacket(packets1[0]) }
        connection1.firstValue.sendPacket { context1.encodePacket(packets1[1]) }
        // * 1 -> 3
        connection1.secondValue.sendPacket { context1.encodePacket(packets1[0]) }
        connection1.secondValue.sendPacket { context1.encodePacket(packets1[1]) }

        // * 2 -> 1
        val packets2 = arrayOf(
                GetBlockAtHeight(20),
                GetBlockAtHeight(21))
        connection2.firstValue.sendPacket { context2.encodePacket(packets2[0]) }
        connection2.firstValue.sendPacket { context2.encodePacket(packets2[1]) }
        // * 2 -> 3
        connection2.secondValue.sendPacket { context2.encodePacket(packets2[0]) }
        connection2.secondValue.sendPacket { context2.encodePacket(packets2[1]) }

        // * 3 -> 1
        val packets3 = arrayOf(
                GetBlockAtHeight(30),
                GetBlockAtHeight(31))
        connection3.firstValue.sendPacket { context3.encodePacket(packets3[0]) }
        connection3.firstValue.sendPacket { context3.encodePacket(packets3[1]) }
        // * 3 -> 2
        connection3.secondValue.sendPacket { context3.encodePacket(packets3[0]) }
        connection3.secondValue.sendPacket { context3.encodePacket(packets3[1]) }

        // * asserting
        await().atMost(TEN_SECONDS)
                .untilAsserted {
                    // Peer1
                    val actualPackets1 = argumentCaptor<ByteArray>()
                    val expected1 = arrayOf(20L, 21L, 30L, 31L)
                    verify(context1.packets, times(4)).invoke(actualPackets1.capture(), any())
                    actualPackets1.allValues
                            .map { (context1.decodePacket(it) as GetBlockAtHeight).height }
                            .forEach { assert(it).isIn(*expected1) }

                    // Peer2
                    val actualPackets2 = argumentCaptor<ByteArray>()
                    val expected2 = arrayOf(10L, 11L, 30L, 31L)
                    verify(context2.packets, times(4)).invoke(actualPackets2.capture(), any())
                    actualPackets2.allValues
                            .map { (context2.decodePacket(it) as GetBlockAtHeight).height }
                            .forEach { assert(it).isIn(*expected2) }

                    // Peer2
                    val actualPackets3 = argumentCaptor<ByteArray>()
                    val expected3 = arrayOf(10L, 11L, 20L, 21L)
                    verify(context3.packets, times(4)).invoke(actualPackets3.capture(), any())
                    actualPackets3.allValues
                            .map { (context2.decodePacket(it) as GetBlockAtHeight).height }
                            .forEach { assert(it).isIn(*expected3) }
                }
    }

}