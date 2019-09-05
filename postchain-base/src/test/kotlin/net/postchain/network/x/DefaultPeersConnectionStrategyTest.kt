package net.postchain.network.x

import net.postchain.base.*
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

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
        val config = object: PeerCommConfigurationDummy() {
            override val networkNodes = NetworkNodes.buildNetworkNodesDummy()
        }

        var portList: MutableList<Int> = mutableListOf<Int>()
        val action: (PeerInfo) -> Unit =  { portList.add(it.port) }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)

        // Then
        assertEquals(0, portList.size)
    }


    @Test
    fun peer1_interaction_when_two_peers_and_pubkey2_provided() {
        val config = object: PeerCommConfigurationDummy() {
            override val networkNodes =NetworkNodes.buildNetworkNodes(setOf(peer1, peer2), XPeerID(pubKey2))
            override val pubKey = pubKey2
        }

        var portList: MutableList<Int> = mutableListOf<Int>()
        val action: (PeerInfo) -> Unit =  { portList.add(it.port) }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)

        // Then
        assertEquals(1, portList.size)
        assertEquals(3331, portList[0])
    }

    @Test
    fun peer1_2_3_interactions_when_four_peers_and_pubkey4_provided() {
        val config = object: PeerCommConfigurationDummy() {
            override val networkNodes =NetworkNodes.buildNetworkNodes(setOf(peer1, peer2, peer3, peer4), XPeerID(pubKey4))
            override val pubKey = pubKey4
        }

        var portList: MutableList<Int> = mutableListOf<Int>()
        val action: (PeerInfo) -> Unit =  { portList.add(it.port) }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)

        // Then
        assertEquals(3, portList.size)
        assertEquals(3331, portList[0])
        assertEquals(3332, portList[1])
        assertEquals(3333, portList[2])
    }

    @Test(expected = IllegalArgumentException::class)
    fun will_result_in_exception_when_myIndex_too_big() {
        val config = object: PeerCommConfigurationDummy() {
            override val networkNodes =NetworkNodes.buildNetworkNodes(setOf(peer1, peer2), XPeerID(ByteArray(0)))
        }

        var portList: MutableList<Int> = mutableListOf<Int>()
        val action: (PeerInfo) -> Unit =  { portList.add(it.port) }

        // When
        DefaultPeersConnectionStrategy.forEach(config, action)
    }
}