package net.postchain.network.x

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import com.nhaarman.mockitokotlin2.*
import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.base.peerId
import net.postchain.network.XPacketDecoder
import net.postchain.network.XPacketEncoder
import org.junit.Before
import org.junit.Test

class DefaultXCommunicationManagerTest {

    private val blockchainRid = byteArrayOf(0x01)
    private lateinit var peerInfo1: PeerInfo
    private lateinit var peerInfo2: PeerInfo

    private val pubKey1 = byteArrayOf(0x01)
    private val pubKey2 = byteArrayOf(0x02)

    companion object {
        private val CHAIN_ID = 1L
    }

    @Before
    fun setUp() {
        // TODO: [et]: Make dynamic ports
        peerInfo1 = PeerInfo("localhost", 3331, pubKey1)
        peerInfo2 = PeerInfo("localhost", 3332, pubKey2)
    }

    @Test
    fun successful_construction_of_CommunicationManager_with_no_peers() {
        // Given
        val connectionManager: XConnectionManager = mock()
        val peerCommunicationConfig: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf()
        }
        val packetEncoder: XPacketEncoder<Int> = mock()
        val packetDecoder: XPacketDecoder<Int> = mock()

        // When
        val communicationManager = DefaultXCommunicationManager(
                connectionManager, peerCommunicationConfig, CHAIN_ID, blockchainRid, packetEncoder, packetDecoder)
        communicationManager.init()

        // Then
        argumentCaptor<XChainPeerConfiguration>().apply {
            verify(connectionManager).connectChain(capture(), eq(true))

            assert(firstValue.chainID).isEqualTo(CHAIN_ID)
            assert(firstValue.commConfiguration).isSameAs(peerCommunicationConfig)
//            val f: XPacketHandler = { _, _ -> ; } // TODO: Assert function types
//            assert(firstValue.packetHandler).isInstanceOf(f.javaClass)
//            assert(firstValue.identPacketConverter).isSameAs(packetConverter)
        }

        communicationManager.shutdown()
    }

    @Test
    fun successful_construction_of_CommunicationManager_with_two_peers() {
        // Given
        val connectionManager: XConnectionManager = mock()
        val peerCommunicationConfig: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf(peerInfo1, peerInfo2)
            on { resolvePeer(peerInfo1.pubKey) } doReturn peerInfo1
            on { resolvePeer(peerInfo2.pubKey) } doReturn peerInfo2
        }
        val packetEncoder: XPacketEncoder<Int> = mock()
        val packetDecoder: XPacketDecoder<Int> = mock()

        // When
        val communicationManager = DefaultXCommunicationManager(
                connectionManager, peerCommunicationConfig, CHAIN_ID, blockchainRid, packetEncoder, packetDecoder)
        communicationManager.init()

        // Then
        argumentCaptor<XChainPeerConfiguration>().apply {
            verify(connectionManager).connectChain(capture(), eq(true))

            assert(firstValue.chainID).isEqualTo(CHAIN_ID)
            assert(firstValue.commConfiguration).isSameAs(peerCommunicationConfig)
//            val f: XPacketHandler = { _, _ -> ; } // TODO: Assert function types
//            assert(firstValue.packetHandler).isInstanceOf(f.javaClass)
//            assert(firstValue.identPacketConverter).isSameAs(packetConverter)
        }

        communicationManager.shutdown()
    }

    @Test(expected = IllegalArgumentException::class)
    fun sendPacket_will_result_in_exception_if_empty_XPeerID_was_given() {
        // Given
        val connectionManager: XConnectionManager = mock()
        val peerCommunicationConfig: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf(peerInfo1, peerInfo2)
        }

        // When
        val communicationManager = DefaultXCommunicationManager<Int>(
                connectionManager, peerCommunicationConfig, CHAIN_ID, blockchainRid, mock(), mock())
        communicationManager.init()
        communicationManager.sendPacket(0, XPeerID(byteArrayOf()))
        communicationManager.shutdown()
    }

    @Test(expected = IllegalArgumentException::class)
    fun sendPacket_will_result_in_exception_if_unknown_recipient_was_given() {
        // Given
        val connectionManager: XConnectionManager = mock()
        val peerCommunicationConfig: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf(peerInfo1, peerInfo2)
        }

        // When
        val communicationManager = DefaultXCommunicationManager<Int>(
                connectionManager, peerCommunicationConfig, CHAIN_ID, blockchainRid, mock(), mock())
        communicationManager.init()
        communicationManager.sendPacket(0, XPeerID(byteArrayOf(0x42)))
        communicationManager.shutdown()
    }

    @Test(expected = IllegalArgumentException::class)
    fun sendPacket_will_result_in_exception_if_my_XPeerID_was_given() {
        // Given
        val peersConfig: PeerCommConfiguration = mock {
            on { myIndex } doReturn 1
            on { peerInfo } doReturn arrayOf(peerInfo1, peerInfo2)
        }

        // When / Then exception
        DefaultXCommunicationManager<Int>(mock(), peersConfig, CHAIN_ID, blockchainRid, mock(), mock())
                .apply {
                    sendPacket(0, XPeerID(pubKey2))
                }
    }

    @Test
    fun sendPacket_sends_packet_successfully() {
        // Given
        val peerInfo1Mock: PeerInfo = spy(peerInfo1)
        val connectionManager: XConnectionManager = mock()
        val peerCommunicationConfig: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf(peerInfo1Mock, peerInfo2)
            on { myIndex } doReturn 1
        }

        // When
        val communicationManager = DefaultXCommunicationManager<Int>(
                connectionManager, peerCommunicationConfig, CHAIN_ID, blockchainRid, mock(), mock()
        )
                .apply {
                    init()
                    sendPacket(0, XPeerID(pubKey1))
                }

        // Then
        verify(connectionManager).sendPacket(any(), eq(CHAIN_ID), eq(peerInfo1.peerId()))
        verify(peerCommunicationConfig, times(2)).peerInfo
        verify(peerInfo1Mock).pubKey

        communicationManager.shutdown()
    }

    @Test
    fun broadcastPacket_sends_packet_successfully() {
        // Given
        val connectionManager: XConnectionManager = mock()

        // When
        val communicationManager = DefaultXCommunicationManager<Int>(
                connectionManager, mock(), CHAIN_ID, blockchainRid, mock(), mock()
        )
                .apply {
                    init()
                    broadcastPacket(42)
                }

        // Then
        verify(connectionManager).broadcastPacket(any(), eq(CHAIN_ID))

        communicationManager.shutdown()
    }

    @Test
    fun shutdown_successfully_disconnects_chain() {
        // Given
        val connectionManager: XConnectionManager = mock()

        // When
        val communicationManager = DefaultXCommunicationManager<Int>(
                connectionManager, mock(), CHAIN_ID, blockchainRid, mock(), mock()
        )
                .apply {
                    init()
                    shutdown()
                }

        // Then
        verify(connectionManager).disconnectChain(eq(CHAIN_ID))

        communicationManager.shutdown()
    }
}