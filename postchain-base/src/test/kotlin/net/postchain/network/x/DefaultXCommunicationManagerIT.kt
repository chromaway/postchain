package net.postchain.network.x

import com.nhaarman.mockitokotlin2.*
import net.postchain.base.PeerInfo
import net.postchain.network.IdentPacketInfo
import net.postchain.network.PacketConverter
import net.postchain.network.netty.NettyConnectorFactory
import org.awaitility.Awaitility.await
import org.awaitility.Duration.FIVE_SECONDS
import org.awaitility.Duration.TWO_SECONDS
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class DefaultXCommunicationManagerIT {

    private val blockchainRid = byteArrayOf(0x01)

    private lateinit var peerInfo1: PeerInfo
    private lateinit var identPacket1: ByteArray

    private lateinit var peerInfo2: PeerInfo
    private lateinit var identPacket2: ByteArray

    @Before
    fun setUp() {
        // TODO: [et]: Make dynamic ports
        peerInfo1 = PeerInfo("localhost", 3331, byteArrayOf(0x01))
        identPacket1 = byteArrayOf(0x01, 0x01)

        peerInfo2 = PeerInfo("localhost", 3332, byteArrayOf(0x02))
        identPacket2 = byteArrayOf(0x02, 0x02)
    }

    @Test
    fun twoPeers_SendsPackets_Successfully() {
        val connectorFactory = NettyConnectorFactory()
        val peerInfos = arrayOf(peerInfo1, peerInfo2)

        // Given
        val packetConverter1: PacketConverter<Int> = mock {
            on { makeIdentPacket(peerInfo2.pubKey) } doReturn identPacket2
            on { parseIdentPacket(identPacket1) } doReturn IdentPacketInfo(peerInfo1.pubKey, blockchainRid)

            on { encodePacket(2) } doReturn byteArrayOf(0x02)
            on { encodePacket(22) } doReturn byteArrayOf(0x02, 0x02)
            on { encodePacket(222) } doReturn byteArrayOf(0x02, 0x02, 0x02)

            onGeneric { decodePacket(peerInfo1.pubKey, byteArrayOf(0x01)) } doReturn 1
            onGeneric { decodePacket(peerInfo1.pubKey, byteArrayOf(0x01, 0x01)) } doReturn 11
        }

        val packetConverter2: PacketConverter<Int> = mock {
            on { makeIdentPacket(peerInfo1.pubKey) } doReturn identPacket1
            on { parseIdentPacket(identPacket2) } doReturn IdentPacketInfo(peerInfo2.pubKey, blockchainRid)

            onGeneric { decodePacket(peerInfo2.pubKey, byteArrayOf(0x02)) } doReturn 2
            onGeneric { decodePacket(peerInfo2.pubKey, byteArrayOf(0x02, 0x02)) } doReturn 22
            onGeneric { decodePacket(peerInfo2.pubKey, byteArrayOf(0x02, 0x02, 0x02)) } doReturn 222

            on { encodePacket(1) } doReturn byteArrayOf(0x01)
            on { encodePacket(11) } doReturn byteArrayOf(0x01, 0x01)
        }

        // When
        val context1 = IntegrationTestContext(connectorFactory, blockchainRid, peerInfos, 0, packetConverter1)
        val context2 = IntegrationTestContext(connectorFactory, blockchainRid, peerInfos, 1, packetConverter2)

        // TODO: [et]: Fix two-connected-nodes problem
        await().atMost(TWO_SECONDS)
                .untilCallTo { context1.communicationManager.connectionManager.getConnectedPeers(1L) }
                .matches { peers -> !peers!!.isEmpty() }

        await().atMost(TWO_SECONDS)
                .untilCallTo { context2.communicationManager.connectionManager.getConnectedPeers(1L) }
                .matches { peers -> !peers!!.isEmpty() }

//        println("getConnectedPeers 1: ${communicationManager1.connectionManager.getConnectedPeers(1L).asString()}")
//        println("getConnectedPeers 1: ${communicationManager2.connectionManager.getConnectedPeers(1L).asString()}")

        // Interactions
        context1.communicationManager.sendPacket(2, setOf(1))

        context2.communicationManager.sendPacket(1, setOf(0))
        context2.communicationManager.sendPacket(11, setOf(0))

        context1.communicationManager.sendPacket(22, setOf(1))
        context1.communicationManager.sendPacket(222, setOf(1))

        // Waiting for all transfers
        TimeUnit.SECONDS.sleep(1)

        // Then
        // - peer1
        verify(packetConverter1).makeIdentPacket(any())
        verify(packetConverter1).parseIdentPacket(any())

        verify(packetConverter1).encodePacket(2)
        verify(packetConverter1).encodePacket(22)
        verify(packetConverter1).encodePacket(222)

        verify(packetConverter1).decodePacket(any(), eq(byteArrayOf(0x01)))
        verify(packetConverter1).decodePacket(any(), eq(byteArrayOf(0x01, 0x01)))

        // - peer2
        verify(packetConverter2).makeIdentPacket(any())
        verify(packetConverter2).parseIdentPacket(any())

        verify(packetConverter2).decodePacket(any(), eq(byteArrayOf(0x02)))
        verify(packetConverter2).decodePacket(any(), eq(byteArrayOf(0x02, 0x02)))
        verify(packetConverter2).decodePacket(any(), eq(byteArrayOf(0x02, 0x02, 0x02)))

        verify(packetConverter2).encodePacket(1)
        verify(packetConverter2).encodePacket(11)

        // - Received packets
        // -- peer2
        await().atMost(FIVE_SECONDS).untilAsserted {
            Assert.assertArrayEquals(
                    arrayOf(2, 22, 222),
                    context2.communicationManager.getPackets().map { it.second }.toTypedArray()
            )
        }

        // -- peer1
        await().atMost(FIVE_SECONDS).untilAsserted {
            Assert.assertArrayEquals(
                    arrayOf(1, 11),
                    context1.communicationManager.getPackets().map { it.second }.toTypedArray()
            )
        }
    }

    @Test
    fun threePeers_SendsPackets_Successfully() {

    }
}