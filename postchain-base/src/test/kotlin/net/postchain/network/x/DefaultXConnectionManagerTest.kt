// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import assertk.assert
import assertk.assertions.isEmpty
import assertk.isContentEqualTo
import com.nhaarman.mockitokotlin2.*
import net.postchain.base.*
import net.postchain.core.ProgrammerMistake
import net.postchain.core.byteArrayKeyOf
import net.postchain.debug.BlockchainProcessName
import net.postchain.network.XPacketDecoderFactory
import net.postchain.network.XPacketEncoderFactory
import org.apache.commons.lang3.reflect.FieldUtils
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultXConnectionManagerTest {

    private val blockchainRid = BlockchainRid(byteArrayOf(0x01))
    private val cryptoSystem = SECP256K1CryptoSystem()
    private lateinit var connectorFactory: XConnectorFactory<Int>

    private lateinit var peerInfo1: PeerInfo
    private lateinit var peerConnectionDescriptor1: XPeerConnectionDescriptor
    private lateinit var packetEncoderFactory: XPacketEncoderFactory<Int>
    private lateinit var packetDecoderFactory: XPacketDecoderFactory<Int>

    private lateinit var peerInfo2: PeerInfo
    private lateinit var peerConnectionDescriptor2: XPeerConnectionDescriptor

    private lateinit var unknownPeerInfo: PeerInfo

    @Before
    fun setUp() {
        // TODO: [et]: Make dynamic ports
        peerInfo1 = PeerInfo("localhost", 3331, byteArrayOf(0x01))
        peerInfo2 = PeerInfo("localhost", 3332, byteArrayOf(0x02))
        unknownPeerInfo = PeerInfo("localhost", 3333, byteArrayOf(0x03))

        peerConnectionDescriptor1 = XPeerConnectionDescriptor(peerInfo1.peerId(), blockchainRid)
        peerConnectionDescriptor2 = XPeerConnectionDescriptor(peerInfo2.peerId(), blockchainRid)

        val connector: XConnector<Int> = mock {
            on { connectPeer(any(), any(), any()) }.doAnswer { } // FYI: Instead of `doNothing` or `doReturn Unit`
        }

        connectorFactory = mock {
            on { createConnector(any(), any(), any()) } doReturn connector
        }

        packetEncoderFactory = mock {
            on { create(any(), any()) } doReturn mock()
        }

        packetDecoderFactory = mock {
            on { create(any()) } doReturn mock()
        }
    }

    @Test
    fun connectChain_without_autoConnect() {
        // Given
        val communicationConfig: PeerCommConfiguration = mock {
            on { myPeerInfo() } doReturn peerInfo1
        }
        val chainPeerConfig: XChainPeerConfiguration = mock {
            on { chainID } doReturn 1L
            on { blockchainRID } doReturn blockchainRid
            on { commConfiguration } doReturn communicationConfig
        }

        // When
        val connectionManager = DefaultXConnectionManager(
                connectorFactory, communicationConfig, packetEncoderFactory, packetDecoderFactory, cryptoSystem
        )
                .also { it.connectChain(chainPeerConfig, false, mock()) }

        // Then
        verify(chainPeerConfig, times(5)).chainID
        verify(chainPeerConfig, times(2 + 1)).blockchainRID
        verify(chainPeerConfig, never()).commConfiguration
        verify(communicationConfig, never()).networkNodes

        connectionManager.shutdown()
    }

    @Test
    fun connectChain_with_autoConnect_without_any_peers_will_result_in_exception() {
        // Given
        val communicationConfig: PeerCommConfiguration = mock {
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodesDummy()
            on { myPeerInfo() } doReturn peerInfo1
        }
        val chainPeerConfig: XChainPeerConfiguration = mock {
            on { chainID } doReturn 1L
            on { blockchainRID } doReturn blockchainRid
            on { commConfiguration } doReturn communicationConfig
        }

        // When
        val connectionManager = DefaultXConnectionManager(
                connectorFactory, communicationConfig, packetEncoderFactory, packetDecoderFactory, cryptoSystem)

        try {
            connectionManager.also { it.connectChain(chainPeerConfig, true, mock()) }
        } catch (e: IllegalArgumentException) {
        }

        // Then
        verify(chainPeerConfig, atLeast(3)).chainID
        verify(chainPeerConfig, times(1)).commConfiguration
        verify(chainPeerConfig, times(2 + 1)).blockchainRID
        verify(communicationConfig).networkNodes

        connectionManager.shutdown()
    }

    @Test
    fun connectChain_with_autoConnect_with_two_peers() {
        // TODO: [et]: Maybe use arg captor here
        // Given
        val communicationConfig: PeerCommConfiguration = mock {
            on { pubKey } doReturn peerInfo2.pubKey// See DefaultPeersConnectionStrategy
            on { myPeerInfo() } doReturn peerInfo2
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodes(setOf(peerInfo1, peerInfo2), XPeerID(peerInfo2.pubKey))
            on { resolvePeer(peerInfo1.pubKey) } doReturn peerInfo1
        }
        val chainPeerConfig: XChainPeerConfiguration = mock {
            on { chainID } doReturn 1L
            on { blockchainRID } doReturn blockchainRid
            on { commConfiguration } doReturn communicationConfig
        }

        // When
        val connectionManager = DefaultXConnectionManager(
                connectorFactory, communicationConfig, packetEncoderFactory, packetDecoderFactory, cryptoSystem
        )
                .also { it.connectChain(chainPeerConfig, true, mock()) }

        // Then
        verify(chainPeerConfig, atLeast(3)).chainID
        verify(chainPeerConfig, times(1 + (2 - 1) * 2)).commConfiguration
        verify(chainPeerConfig, times(1 + 1 * 2 + 1 + 2)).blockchainRID

        connectionManager.shutdown()
    }

    @Test(expected = ProgrammerMistake::class)
    fun connectChainPeer_will_result_in_exception_if_chain_is_not_connected() {
        emptyManager().connectChainPeer(1, peerInfo1.peerId())
    }

    @Test(expected = ProgrammerMistake::class)
    fun connectChainPeer_connects_unknown_peer_with_exception() {
        // Given
        val communicationConfig: PeerCommConfiguration = emptyCommConf()
        val chainPeerConfig: XChainPeerConfiguration = mock {
            on { chainID } doReturn 1L
            on { blockchainRID } doReturn blockchainRid
            on { commConfiguration } doReturn communicationConfig
        }

        // When / Then exception
        DefaultXConnectionManager(
                connectorFactory, communicationConfig, mock(), mock(), cryptoSystem
        ).apply {
            connectChain(chainPeerConfig, false, mock()) // Without connecting to peers
            connectChainPeer(1, unknownPeerInfo.peerId())
        }
    }

    @Test
    fun connectChainPeer_connects_peer_successfully() {
        // Given
        val communicationConfig: PeerCommConfiguration = mock {
            on { pubKey } doReturn peerInfo1.pubKey
            on { myPeerInfo() } doReturn peerInfo1
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodes(setOf(peerInfo1, peerInfo2), XPeerID(peerInfo1.pubKey))
            on { resolvePeer(peerInfo2.pubKey) } doReturn peerInfo2
        }
        val chainPeerConfig: XChainPeerConfiguration = mock {
            on { chainID } doReturn 1L
            on { blockchainRID } doReturn blockchainRid
            on { commConfiguration } doReturn communicationConfig
        }

        val loggerName = BlockchainProcessName(
                peerInfo1.pubKey.byteArrayKeyOf().toString(), blockchainRid
        ).toString()

        // When
        val connectionManager = DefaultXConnectionManager(
                connectorFactory, communicationConfig, packetEncoderFactory, packetDecoderFactory, cryptoSystem
        )
                .apply {
                    connectChain(chainPeerConfig, false, mock()) // Without connecting to peers
                    connectChainPeer(1, peerInfo2.peerId())
                }

        // Then
        verify(chainPeerConfig, atLeast(3)).chainID
        verify(chainPeerConfig, times(2)).commConfiguration
        verify(chainPeerConfig, times(1 + 2 + 1 + 2)).blockchainRID

        connectionManager.shutdown()
    }

    @Test
    fun connectChainPeer_connects_already_connected_peer_and_nothing_happens() {
        // Given
        val communicationConfig: PeerCommConfiguration = mock {
            on { pubKey } doReturn peerInfo2.pubKey // See DefaultPeersConnectionStrategy
            on { myPeerInfo() } doReturn peerInfo2
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodes(setOf(peerInfo1, peerInfo2), XPeerID(peerInfo2.pubKey))
            on { resolvePeer(peerInfo1.pubKey) } doReturn peerInfo1
        }
        val chainPeerConfig: XChainPeerConfiguration = mock {
            on { chainID } doReturn 1L
            on { blockchainRID } doReturn blockchainRid
            on { commConfiguration } doReturn communicationConfig
        }

        // When
        val connectionManager = DefaultXConnectionManager(
                connectorFactory, communicationConfig, packetEncoderFactory, packetDecoderFactory, cryptoSystem
        ).apply {
            connectChain(chainPeerConfig, true, mock()) // Auto connect all peers

            // Emulates call of onPeerConnected() by XConnector
            onPeerConnected(mockConnection(peerConnectionDescriptor1))

            connectChainPeer(1, peerInfo1.peerId())
        }

        // Then
        verify(chainPeerConfig, atLeast(3)).chainID
        verify(chainPeerConfig, times(1 + (2 - 1) * 2)).commConfiguration
        verify(chainPeerConfig, times(1 + 2 + 1 + 2)).blockchainRID

        connectionManager.shutdown()
    }

    @Test(expected = ProgrammerMistake::class)
    fun disconnectChainPeer_will_result_in_exception_if_chain_is_not_connected() {
        emptyManager().disconnectChainPeer(1L, peerInfo1.peerId())
    }

    @Test
    fun disconnectChain_wont_result_in_exception_if_chain_is_not_connected() {
        emptyManager().disconnectChain(1, mock())
    }

    @Test(expected = ProgrammerMistake::class)
    fun isPeerConnected_will_result_in_exception_if_chain_is_not_connected() {
        emptyManager().isPeerConnected(1, peerInfo1.peerId())
    }

    @Test(expected = ProgrammerMistake::class)
    fun getConnectedPeers_will_result_in_exception_if_chain_is_not_connected() {
        emptyManager().getConnectedPeers(1)
    }

    @Test
    fun isPeerConnected_and_getConnectedPeers_are_succeeded() {
        // Given
        val communicationConfig: PeerCommConfiguration = mock {
            on { pubKey } doReturn peerInfo2.pubKey // See DefaultPeersConnectionStrategy
            on { myPeerInfo() } doReturn peerInfo2
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodes(setOf(peerInfo1, peerInfo2), XPeerID(peerInfo2.pubKey))
            on { resolvePeer(peerInfo1.pubKey) } doReturn peerInfo1
        }
        val chainPeerConfig: XChainPeerConfiguration = mock {
            on { chainID } doReturn 1L
            on { blockchainRID } doReturn blockchainRid
            on { commConfiguration } doReturn communicationConfig
        }

        // When
        val connectionManager = DefaultXConnectionManager(
                connectorFactory, communicationConfig, packetEncoderFactory, packetDecoderFactory, cryptoSystem
        ).apply {
            connectChain(chainPeerConfig, true, mock()) // With autoConnect

            // Then / before peers connected
            // - isPeerConnected
            assertFalse { isPeerConnected(1L, peerInfo1.peerId()) }
            assertFalse { isPeerConnected(1L, peerInfo2.peerId()) }
            assertFalse { isPeerConnected(1L, unknownPeerInfo.peerId()) }
            // - getConnectedPeers
            assert(getConnectedPeers(1L).toTypedArray()).isEmpty()

            // Emulates call of onPeerConnected() by XConnector
            onPeerConnected(mockConnection(peerConnectionDescriptor1))
            onPeerConnected(mockConnection(peerConnectionDescriptor2))

            // Then / after peers connected
            // - isPeerConnected
            assertTrue { isPeerConnected(1L, peerInfo1.peerId()) }
            assertTrue { isPeerConnected(1L, peerInfo2.peerId()) }
            assertFalse { isPeerConnected(1L, unknownPeerInfo.peerId()) }
            // - getConnectedPeers
            assert(getConnectedPeers(1L).toTypedArray()).isContentEqualTo(
                    arrayOf(peerInfo1.peerId(), peerInfo2.peerId()))


            // When / Disconnecting peer1
            disconnectChainPeer(1L, peerInfo1.peerId())
            // Then
            // - isPeerConnected
            assertFalse { isPeerConnected(1L, peerInfo1.peerId()) }
            assertTrue { isPeerConnected(1L, peerInfo2.peerId()) }
            assertFalse { isPeerConnected(1L, unknownPeerInfo.peerId()) }
            // - getConnectedPeers
            assert(getConnectedPeers(1L).toTypedArray()).isContentEqualTo(
                    arrayOf(peerInfo2.peerId()))


            // When / Disconnecting the whole chain
            disconnectChain(1L, mock())
            // Then
            val internalChains = FieldUtils.readField(this, "chains", true) as Map<*, *>
            assertTrue { internalChains.isEmpty() }
        }

        connectionManager.shutdown()
    }

    @Test(expected = ProgrammerMistake::class)
    fun sendPacket_will_result_in_exception_if_chain_is_not_connected() {
        emptyManager().sendPacket({ byteArrayOf() }, 1, peerInfo2.peerId())
    }

    @Test
    fun sendPacket_sends_packet_to_receiver_via_connection_successfully() {
        // Given
        val communicationConfig: PeerCommConfiguration = mock {
            on { pubKey } doReturn peerInfo2.pubKey // See DefaultPeersConnectionStrategy
            on { myPeerInfo() } doReturn peerInfo2
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodes(setOf(peerInfo1, peerInfo2), XPeerID(peerInfo2.pubKey))
            on { resolvePeer(peerInfo1.pubKey) } doReturn peerInfo1
        }
        val chainPeerConfig: XChainPeerConfiguration = mock {
            on { chainID } doReturn 1L
            on { blockchainRID } doReturn blockchainRid
            on { commConfiguration } doReturn communicationConfig
        }
        val connection1: XPeerConnection = mockConnection(peerConnectionDescriptor1)
        val connection2: XPeerConnection = mockConnection(peerConnectionDescriptor2)

        // When
        val connectionManager = DefaultXConnectionManager(
                connectorFactory, communicationConfig, packetEncoderFactory, packetDecoderFactory, cryptoSystem
        ).apply {
            connectChain(chainPeerConfig, true, mock()) // With autoConnect

            // Emulates call of onPeerConnected() by XConnector
            onPeerConnected(connection1)
            onPeerConnected(connection2)

            sendPacket({ byteArrayOf(0x04, 0x02) }, 1L, peerInfo2.peerId())
        }

        // Then / verify and assert
        verify(connection1, times(0)).sendPacket(any())
        argumentCaptor<LazyPacket>().apply {
            verify(connection2, times(1)).sendPacket(capture())
            assert(firstValue()).isContentEqualTo(byteArrayOf(0x04, 0x02))
        }

        connectionManager.shutdown()
    }

    @Test(expected = ProgrammerMistake::class)
    fun broadcastPacket_will_result_in_exception_if_chain_is_not_connected() {
        emptyManager().broadcastPacket({ byteArrayOf() }, 1)
    }

    private fun emptyManager() = DefaultXConnectionManager(connectorFactory, emptyCommConf(), mock(), mock(), cryptoSystem)

    private fun emptyCommConf(): PeerCommConfiguration {
        return mock {
            on { myPeerInfo() } doReturn peerInfo1
        }
    }

    @Test
    fun broadcastPacket_sends_packet_to_all_receivers_successfully() {
        // Given
        val communicationConfig: PeerCommConfiguration = mock {
            on { pubKey } doReturn peerInfo1.pubKey
            on { myPeerInfo() } doReturn peerInfo1
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodes(setOf(peerInfo1, peerInfo2), XPeerID(peerInfo1.pubKey))
            on { resolvePeer(peerInfo2.pubKey) } doReturn peerInfo2
        }
        val chainPeerConfig: XChainPeerConfiguration = mock {
            on { chainID } doReturn 1L
            on { blockchainRID } doReturn blockchainRid
            on { commConfiguration } doReturn communicationConfig
        }
        val connection1: XPeerConnection = mockConnection(peerConnectionDescriptor1)
        val connection2: XPeerConnection = mockConnection(peerConnectionDescriptor2)

        // When
        val connectionManager = DefaultXConnectionManager(
                connectorFactory, communicationConfig, packetEncoderFactory, packetDecoderFactory, cryptoSystem
        ).apply {
            connectChain(chainPeerConfig, true, mock()) // With autoConnect

            // Emulates call of onPeerConnected() by XConnector
            onPeerConnected(connection1)
            onPeerConnected(connection2)

            broadcastPacket({ byteArrayOf(0x04, 0x02) }, 1L)
        }

        // Then / verify and assert
        argumentCaptor<LazyPacket>().apply {
            verify(connection1, times(1)).sendPacket(capture())
            assert(firstValue()).isContentEqualTo(byteArrayOf(0x04, 0x02))
        }
        argumentCaptor<LazyPacket>().apply {
            verify(connection2, times(1)).sendPacket(capture())
            assert(firstValue()).isContentEqualTo(byteArrayOf(0x04, 0x02))
        }

        connectionManager.shutdown()
    }

    fun mockConnection(descriptor: XPeerConnectionDescriptor): XPeerConnection {
        val m: XPeerConnection = mock()
        whenever(m.descriptor()).thenReturn(descriptor)
        return m
    }
}