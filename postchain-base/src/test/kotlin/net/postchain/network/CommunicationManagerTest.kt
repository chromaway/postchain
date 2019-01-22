package net.postchain.network

import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.devtools.EasyMockUtils.anyOfType
import org.easymock.EasyMock.*
import org.junit.Test

class CommunicationManagerTest {

    @Test
    fun successful_construction_of_CommunicationManager_with_no_peers() {
        // Mock
        val peerCommConfiguration = mock(PeerCommConfiguration::class.java)
        val connectionManager = mock(PeerConnectionManagerInterface::class.java) as PeerConnectionManagerInterface<Int>

        // Given
        // - peerCommConfiguration
        expect(peerCommConfiguration.peerInfo).andReturn(arrayOf()).times(1)
        expect(peerCommConfiguration.myIndex).andReturn(0).times(1)
        expect(peerCommConfiguration.blockchainRID).andReturn(byteArrayOf()).times(1)

        // - connectionManager
        expect(connectionManager.registerBlockchain(
                anyOfType(ByteArray::class.java),
                anyOfType(BlockchainDataHandler::class.java))
        ).times(1)

        replay(peerCommConfiguration)
        replay(connectionManager)

        // When
        CommManager(peerCommConfiguration, connectionManager)

        // Then
        verify(peerCommConfiguration)
        verify(connectionManager)
    }

    @Test
    fun successful_construction_of_CommunicationManager_with_two_peers() {
        // Mock
        val peerCommConfiguration = mock(PeerCommConfiguration::class.java)
        val connectionManager = mock(PeerConnectionManagerInterface::class.java) as PeerConnectionManagerInterface<Int>
        val peerConnection = mock(AbstractPeerConnection::class.java)
        val peer1 = PeerInfo("host1", 1, byteArrayOf(0x01))
        val peer2 = PeerInfo("host2", 2, byteArrayOf(0x02))

        // Given
        // - peerCommConfiguration
        expect(peerCommConfiguration.peerInfo).andReturn(arrayOf(peer1, peer2)).times(1)
        expect(peerCommConfiguration.myIndex).andReturn(10).times(1)
        expect(peerCommConfiguration.blockchainRID).andReturn(byteArrayOf()).times(1)

        // - connectionManager
        expect(connectionManager.registerBlockchain(
                anyOfType(ByteArray::class.java),
                anyOfType(BlockchainDataHandler::class.java))
        ).times(1)

        val handler: (ByteArray) -> Unit = { }
        expect(connectionManager.connectPeer(eq(peer1), anyOfType(handler::class.java)))
                .andReturn(peerConnection).times(1)
        expect(connectionManager.connectPeer(eq(peer2), anyOfType(handler::class.java)))
                .andReturn(peerConnection).times(1)

        replay(peerCommConfiguration)
        replay(connectionManager)

        // When
        CommManager(peerCommConfiguration, connectionManager)

        // Then
        verify(peerCommConfiguration)
        verify(connectionManager)
    }
}