package net.postchain.network.x

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.secp256k1_derivePubKey
import net.postchain.network.IdentPacketInfo
import net.postchain.network.PacketConverter
import net.postchain.network.netty.NettyConnectorFactory
import net.postchain.network.netty.NettyIO
import org.awaitility.Awaitility.await
import org.awaitility.Duration.FIVE_SECONDS
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit

class DefaultXCommunicationManager2IT {

    private val blockchainRid = byteArrayOf(0x01)

    private lateinit var peerInfo1: PeerInfo
    private lateinit var identPacket1: ByteArray

    private lateinit var peerInfo2: PeerInfo
    private lateinit var identPacket2: ByteArray

    private val privateKey = "3132333435363738393031323334353637383930313233343536373839303131".toByteArray()
    private val privateKey2 = "3132333435363738393031323334353637383930313233343536373839303132".toByteArray()
    private val publicKey = secp256k1_derivePubKey(privateKey)
    private val publicKey2 = secp256k1_derivePubKey(privateKey2)

    private val ephemeralKey = SECP256K1CryptoSystem().getRandomBytes(NettyIO.keySizeBytes)
    private val ephemeralPubKey = secp256k1_derivePubKey(ephemeralKey)

    @Before
    fun setUp() {
        // TODO: [et]: Make dynamic ports
        peerInfo1 = PeerInfo("localhost", 3331, publicKey, privateKey)
        identPacket1 = byteArrayOf(0x01, 0x01)

        peerInfo2 = PeerInfo("localhost", 3332, publicKey2, privateKey2)
        identPacket2 = byteArrayOf(0x02, 0x02)
    }

    @Test
    @Ignore
    fun twoPeers_SendsPackets_Successfully() {
        val connectorFactory = NettyConnectorFactory<PacketConverter<Int>>()
        val peerInfos = arrayOf(peerInfo1, peerInfo2)

        // Given
        val packetConverter1: PacketConverter<Int> = mock {
            on { makeIdentPacket(any()) } doReturn identPacket2
            on { parseIdentPacket(any()) } doReturn IdentPacketInfo(peerInfo1.pubKey, blockchainRid, ephemeralPubKey, ephemeralPubKey)

            on { encodePacket(2) } doReturn byteArrayOf(0x02)
            on { encodePacket(22) } doReturn byteArrayOf(0x02, 0x02)
            on { encodePacket(222) } doReturn byteArrayOf(0x02, 0x02, 0x02)

            onGeneric { decodePacket(peerInfo1.pubKey, byteArrayOf(0x01)) } doReturn 1
            onGeneric { decodePacket(peerInfo1.pubKey, byteArrayOf(0x01, 0x01)) } doReturn 11
        }

        val packetConverter2: PacketConverter<Int> = mock {
            on { makeIdentPacket(any()) } doReturn identPacket1
            on { parseIdentPacket(any()) } doReturn IdentPacketInfo(peerInfo2.pubKey, blockchainRid, ephemeralPubKey, ephemeralPubKey)

            onGeneric { decodePacket(peerInfo2.pubKey, byteArrayOf(0x02)) } doReturn 2
            onGeneric { decodePacket(peerInfo2.pubKey, byteArrayOf(0x02, 0x02)) } doReturn 22
            onGeneric { decodePacket(peerInfo2.pubKey, byteArrayOf(0x02, 0x02, 0x02)) } doReturn 222

            on { encodePacket(1) } doReturn byteArrayOf(0x01)
            on { encodePacket(11) } doReturn byteArrayOf(0x01, 0x01)
        }

        // When
        val context1 = IntegrationTestContext(mock()/*connectorFactory*/, peerInfos, 0)
        val context2 = IntegrationTestContext(mock()/*connectorFactory*/, peerInfos, 1)

        // TODO: [et]: Fix two-connected-nodes problem
        await().atMost(FIVE_SECONDS)
                .untilCallTo { context1.communicationManager.connectionManager.getConnectedPeers(1L) }
                .matches { peers -> !peers!!.isEmpty() }

        await().atMost(FIVE_SECONDS)
                .untilCallTo { context2.communicationManager.connectionManager.getConnectedPeers(1L) }
                .matches { peers -> !peers!!.isEmpty() }
        Thread.sleep(3000)

//        println("getConnectedPeers 1: ${communicationManager1.connectionManager.getConnectedPeers(1L).asString()}")
//        println("getConnectedPeers 1: ${communicationManager2.connectionManager.getConnectedPeers(1L).asString()}")

        // Interactions
        context1.communicationManager.sendPacket(2, XPeerID(publicKey2))

        context2.communicationManager.sendPacket(1, XPeerID(publicKey))
        context2.communicationManager.sendPacket(11, XPeerID(publicKey))

        context1.communicationManager.sendPacket(22, XPeerID(publicKey2))
        context1.communicationManager.sendPacket(222, XPeerID(publicKey2))

        // Waiting for all transfers
        TimeUnit.SECONDS.sleep(1)

        // Then
        // - peer1
        // verify(packetConverter1).makeIdentPacket(any())
        //  verify(packetConverter1).parseIdentPacket(any())

        verify(packetConverter1).encodePacket(2)
//        verify(packetConverter1).encodePacket(22)
//        verify(packetConverter1).encodePacket(222)

//        verify(packetConverter1).decodePacket(any(), eq(byteArrayOf(0x01)))
//        verify(packetConverter1).decodePacket(any(), eq(byteArrayOf(0x01, 0x01)))
//
//        // - peer2
//        verify(packetConverter2).makeIdentPacket(any())
//        verify(packetConverter2).parseIdentPacket(any())
//
//        verify(packetConverter2).decodePacket(any(), eq(byteArrayOf(0x02)))
//        verify(packetConverter2).decodePacket(any(), eq(byteArrayOf(0x02, 0x02)))
//        verify(packetConverter2).decodePacket(any(), eq(byteArrayOf(0x02, 0x02, 0x02)))
//
//        verify(packetConverter2).encodePacket(1)
//        verify(packetConverter2).encodePacket(11)
//
//        // - Received packets
//        // -- peer2
//        await().atMost(FIVE_SECONDS).untilAsserted {
//            Assert.assertArrayEquals(
//                    arrayOf(2, 22, 222),
//                    context2.communicationManager.getPackets().map { it.second }.toTypedArray()
//            )
//        }
//
//        // -- peer1
//        await().atMost(FIVE_SECONDS).untilAsserted {
//            Assert.assertArrayEquals(
//                    arrayOf(1, 11),
//                    context1.communicationManager.getPackets().map { it.second }.toTypedArray()
//            )
//        }

        context1.shutdown()
        context2.shutdown()
    }

    @Test
    @Ignore
    fun threePeers_SendsPackets_Successfully() {

    }
}