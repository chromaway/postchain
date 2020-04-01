// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import net.postchain.base.NetworkNodes
import net.postchain.base.PeerInfo
import net.postchain.common.hexStringToByteArray
import net.postchain.core.UserMistake
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DefaultPeersConnectionStrategyTest {

    private lateinit var peer1: PeerInfo
    private lateinit var peer2: PeerInfo
    private lateinit var peer3: PeerInfo
    private lateinit var peer4: PeerInfo

    private lateinit var pubKey1: ByteArray

    private lateinit var pubKey2: ByteArray

    private lateinit var pubKey3: ByteArray

    private lateinit var pubKey4: ByteArray

    @Before
    fun setUp() {
        pubKey1 = "11111111111111111111111111111111".hexStringToByteArray()
        pubKey2 = "22222222222222222222222222222212".hexStringToByteArray()
        pubKey3 = "33333333333333333333333333333333".hexStringToByteArray()
        pubKey4 = "44444444444444444444444444444444".hexStringToByteArray()

        peer1 = PeerInfo("localhost", 3331, pubKey1)
        peer2 = PeerInfo("localhost", 3332, pubKey2)
        peer3 = PeerInfo("localhost", 3333, pubKey3)
        peer4 = PeerInfo("localhost", 3334, pubKey4)
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

    @Test(expected = UserMistake::class)
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