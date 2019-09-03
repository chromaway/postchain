package net.postchain.network.x

import assertk.assert
import assertk.assertions.isEqualTo
import com.nhaarman.mockitokotlin2.*
import net.postchain.base.*
import org.junit.Before
import org.junit.Test

class DefaultPeersConnectionStrategyTest {

    private lateinit var peer1: PeerInfo
    private lateinit var peer2: PeerInfo
    private lateinit var peer3: PeerInfo
    private lateinit var peer4: PeerInfo

    private lateinit var privKey1: ByteArray
    private lateinit var pubKey1: ByteArray

    private lateinit var privKey2: ByteArray
    private lateinit var pubKey2: ByteArray

    private lateinit var privKey3: ByteArray
    private lateinit var pubKey3: ByteArray

    private lateinit var privKey4: ByteArray
    private lateinit var pubKey4: ByteArray

    @Before
    fun setUp() {
        privKey1 = SECP256K1CryptoSystem().getRandomBytes(32)
        pubKey1 = secp256k1_derivePubKey(privKey1)

        privKey2 = SECP256K1CryptoSystem().getRandomBytes(32)
        pubKey2 = secp256k1_derivePubKey(privKey2)

        privKey3 = SECP256K1CryptoSystem().getRandomBytes(32)
        pubKey3 = secp256k1_derivePubKey(privKey3)

        privKey4 = SECP256K1CryptoSystem().getRandomBytes(32)
        pubKey4 = secp256k1_derivePubKey(privKey4)

        peer1 = PeerInfo("localhost", 3331, pubKey1)
        peer2 = PeerInfo("localhost", 3332, pubKey2)
        peer3 = PeerInfo("localhost", 3333, pubKey3)
        peer4 = PeerInfo("localhost", 3334, pubKey4)
    }

    @Test(expected = IllegalArgumentException::class)
    fun will_result_in_exception_when_no_peers_provided() {
        // Mock
        val config: PeerCommConfiguration = mock {
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodesDummy()
        }
        val action: (PeerInfo) -> Unit = mock { Unit }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)

        // Then
        verify(action, never()).invoke(any())
    }

    @Test
    fun no_peers_interactions_when_two_peers_and_pubkey1_provided() {
        // Mock
        val x = NetworkNodes.buildNetworkNodes(setOf(peer1, peer2), XPeerID(pubKey1))
        val config: PeerCommConfiguration = mock {
            on { networkNodes } doReturn x
        }
        val action: (PeerInfo) -> Unit = mock { Unit }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)

        // Then
        verify(action, never()).invoke(any())
    }

    @Test
    fun peer1_interaction_when_two_peers_and_pubkey2_provided() {
        // Mock
        val config: PeerCommConfiguration = mock {
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodes(setOf(peer1, peer2), XPeerID(pubKey2))
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
    fun peer1_2_3_interactions_when_four_peers_and_pubkey4_provided() {
        // Mock
        val config: PeerCommConfiguration = mock {
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodes(setOf(peer1, peer2, peer3, peer4), XPeerID(pubKey4))
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

    @Test(expected = KotlinNullPointerException::class)
    fun will_result_in_exception_when_myIndex_too_big() {
        // Mock
        val config: PeerCommConfiguration = mock {
            on { networkNodes } doReturn NetworkNodes.buildNetworkNodes(setOf(peer1, peer2), XPeerID(ByteArray(0)))
        }

        val action: (PeerInfo) -> Unit = mock { Unit }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)
    }
}