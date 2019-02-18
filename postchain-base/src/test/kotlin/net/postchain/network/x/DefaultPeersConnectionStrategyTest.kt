package net.postchain.network.x

import assertk.assert
import assertk.assertions.isEqualTo
import com.nhaarman.mockitokotlin2.*
import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.secp256k1_derivePubKey
import org.junit.Before
import org.junit.Test

class DefaultPeersConnectionStrategyTest {

    private lateinit var peer1: PeerInfo
    private lateinit var peer2: PeerInfo
    private lateinit var peer3: PeerInfo
    private lateinit var peer4: PeerInfo

    @Before
    fun setUp() {
        val privKey1 = SECP256K1CryptoSystem().getRandomBytes(32)
        val pubKey1 = secp256k1_derivePubKey(privKey1)

        peer1 = PeerInfo("localhost", 3331, pubKey1)
        peer2 = PeerInfo("localhost", 3332, pubKey1)
        peer3 = PeerInfo("localhost", 3333, pubKey1)
        peer4 = PeerInfo("localhost", 3334, pubKey1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun will_result_in_exception_when_no_peers_provided() {
        // Mock
        val config: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf()
        }
        val action: (PeerInfo) -> Unit = mock { Unit }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)

        // Then
        verify(action, never()).invoke(any())
    }

    @Test
    fun no_peers_interactions_when_two_peers_and_myIndex_0_provided() {
        // Mock
        val config: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf(peer1, peer2)
            on { myIndex } doReturn 0
        }
        val action: (PeerInfo) -> Unit = mock { Unit }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)

        // Then
        verify(action, never()).invoke(any())
    }

    @Test
    fun peer1_interaction_when_two_peers_and_myIndex_1_provided() {
        // Mock
        val config: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf(peer1, peer2)
            on { myIndex } doReturn 1
        }

        val action: (PeerInfo) -> Unit = mock { Unit }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)

        // Then
        argumentCaptor<PeerInfo>().apply {
            verify(action).invoke(capture())
            assert(firstValue.port).isEqualTo(3331)
        }
    }

    @Test
    fun peer1_2_3_interactions_when_four_peers_and_myIndex_3_provided() {
        // Mock
        val config: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf(peer1, peer2, peer3, peer4)
            on { myIndex } doReturn 3
        }

        val action: (PeerInfo) -> Unit = mock { Unit }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)

        // Then
        argumentCaptor<PeerInfo>().apply {
            verify(action, times(3)).invoke(capture())
            assert(firstValue.port).isEqualTo(3331)
            assert(secondValue.port).isEqualTo(3332)
            assert(thirdValue.port).isEqualTo(3333)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun will_result_in_exception_when_myIndex_too_big() {
        // Mock
        val config: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf(peer1, peer2)
            on { myIndex } doReturn 42
        }

        val action: (PeerInfo) -> Unit = mock { Unit }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)
    }

    @Test(expected = IllegalArgumentException::class)
    fun will_result_in_exception_when_myIndex_negative() {
        // Mock
        val config: PeerCommConfiguration = mock {
            on { peerInfo } doReturn arrayOf(peer1, peer2)
            on { myIndex } doReturn -1
        }

        val action: (PeerInfo) -> Unit = mock { Unit }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)
    }
}