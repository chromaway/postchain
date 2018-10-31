package net.postchain.network.x

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import com.nhaarman.mockitokotlin2.*
import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.base.peerId
import net.postchain.network.PacketConverter
import org.junit.Before
import org.junit.Test

class DefaultXCommunicationManagerTest {

    private val blockchainRid = byteArrayOf(0x01)
    private lateinit var peerInfo1: PeerInfo
    private lateinit var peerInfo2: PeerInfo

    @Before
    fun setUp() {
        // TODO: [et]: Make dynamic ports
        peerInfo1 = PeerInfo("localhost", 3331, byteArrayOf(0x01))
        peerInfo2 = PeerInfo("localhost", 3332, byteArrayOf(0x02))
    }

    @Test
    fun successful_construction_of_CommunicationManager_with_no_peers() {
        // Given
        val connectionManager: XConnectionManager = mock()
        val peerCommunicationConfig: PeerCommConfiguration = mock {
            on { blockchainRID } doReturn blockchainRid
            on { peerInfo } doReturn arrayOf()
        }
        val packetConverter: PacketConverter<Int> = mock()

        // When
        DefaultXCommunicationManager(connectionManager, peerCommunicationConfig, 1L, packetConverter)

        // Then
        argumentCaptor<XChainPeerConfiguration>().apply {
            verify(connectionManager).connectChain(capture(), eq(true))

            assert(firstValue.chainID).isEqualTo(1L)
            assert(firstValue.commConfiguration).isSameAs(peerCommunicationConfig)
//            val f: XPacketHandler = { _, _ -> ; } // TODO: Assert function types
//            assert(firstValue.packetHandler).isInstanceOf(f.javaClass)
            assert(firstValue.identPacketConverter).isSameAs(packetConverter)
        }
    }

    @Test
    fun successful_construction_of_CommunicationManager_with_two_peers() {
        // Given
        val connectionManager: XConnectionManager = mock()
        val peerCommunicationConfig: PeerCommConfiguration = mock {
            on { blockchainRID } doReturn blockchainRid
            on { peerInfo } doReturn arrayOf(peerInfo1, peerInfo2)
            on { resolvePeer(peerInfo1.pubKey) } doReturn peerInfo1
            on { resolvePeer(peerInfo2.pubKey) } doReturn peerInfo2
        }
        val packetConverter: PacketConverter<Int> = mock()

        // When
        DefaultXCommunicationManager(connectionManager, peerCommunicationConfig, 1L, packetConverter)

        // Then
        argumentCaptor<XChainPeerConfiguration>().apply {
            verify(connectionManager).connectChain(capture(), eq(true))

            assert(firstValue.chainID).isEqualTo(1L)
            assert(firstValue.commConfiguration).isSameAs(peerCommunicationConfig)
//            val f: XPacketHandler = { _, _ -> ; } // TODO: Assert function types
//            assert(firstValue.packetHandler).isInstanceOf(f.javaClass)
            assert(firstValue.identPacketConverter).isSameAs(packetConverter)
        }
    }

    @Test(expected = AssertionError::class)
    fun sendPacket_will_result_in_exception_if_no_recipients_was_given() {
        // When / Then exception
        DefaultXCommunicationManager(mock(), mock(), 1L, mock<PacketConverter<Int>>()).apply {
            sendPacket(0, setOf())
        }
    }

    @Test(expected = AssertionError::class)
    fun sendPacket_will_result_in_exception_if_more_then_one_recipients_was_given() {
        // When / Then exception
        DefaultXCommunicationManager(mock(), mock(), 1L, mock<PacketConverter<Int>>()).apply {
            sendPacket(0, setOf(0, 42))
        }
    }

    @Test
    fun sendPacket_sends_packet_successfully() {
        // Given
        val peerInfo1Mock: PeerInfo = spy(peerInfo1)
        val connectionManager: XConnectionManager = mock()
        val peerCommunicationConfig: PeerCommConfiguration = mock {
            on { blockchainRID } doReturn blockchainRid
            on { peerInfo } doReturn arrayOf(peerInfo1Mock, peerInfo2)
        }

        // When
        DefaultXCommunicationManager(connectionManager, peerCommunicationConfig, 1L, mock<PacketConverter<Int>>()).apply {
            sendPacket(0, setOf(0))
        }

        // Then
        verify(connectionManager).sendPacket(any(), eq(1L), eq(peerInfo1.peerId()))
        verify(peerCommunicationConfig).peerInfo
        verify(peerInfo1Mock).pubKey
    }

    @Test
    fun broadcastPacket_sends_packet_successfully() {
        // Given
        val connectionManager: XConnectionManager = mock()

        // When
        DefaultXCommunicationManager(connectionManager, mock(), 1L, mock<PacketConverter<Int>>()).apply {
            broadcastPacket(42)
        }

        // Then
        verify(connectionManager).broadcastPacket(any(), eq(1L))
    }

    @Test
    fun shutdown_successfully_disconnects_chain() {
        // Given
        val connectionManager: XConnectionManager = mock()

        // When
        DefaultXCommunicationManager(connectionManager, mock(), 1L, mock<PacketConverter<Int>>()).apply {
            shutdown()
        }

        // Then
        verify(connectionManager).disconnectChain(eq(1L))
    }
}