// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import com.nhaarman.mockitokotlin2.*
import mu.KLogging
import net.postchain.common.hexStringToByteArray
import org.awaitility.Awaitility
import org.junit.Test
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class DefaultPeersConnectionStrategyTest {

    companion object: KLogging()

    val peer1 = XPeerID("111111".hexStringToByteArray())
    val peer2 = XPeerID("222222".hexStringToByteArray())
    val peer3 = XPeerID("333333".hexStringToByteArray())
    val peer4 = XPeerID("444444".hexStringToByteArray())
    val peerCaptor = argumentCaptor<XPeerID>()
    val peerCaptor2 = argumentCaptor<XPeerID>()
    val chainCaptor = argumentCaptor<Long>()
    val connMan : XConnectionManager = mock()

    fun testConnectAll(me: XPeerID, peerIds: Set<XPeerID>, expectedConns: Set<XPeerID>): DefaultPeersConnectionStrategy {
        val strategy = sut(me)
        strategy.connectAll(0, peerIds)

        verify(connMan, times(expectedConns.size)).connectChainPeer(chainCaptor.capture(), peerCaptor.capture())
        assertEquals(expectedConns, peerCaptor.allValues.toSet())

        reset(connMan)
        val expectedResidual = peerIds.subtract(expectedConns)
        whenever(connMan.getConnectedPeers(0)).thenReturn(expectedConns.toList())

        Awaitility.await().atMost(1, TimeUnit.SECONDS).untilAsserted {
            verify(connMan, times(expectedResidual.size)).connectChainPeer(chainCaptor.capture(), peerCaptor2.capture())
            assertEquals(expectedResidual, peerCaptor2.allValues.toSet())
        }
        return strategy
    }

    private fun sut(me: XPeerID): DefaultPeersConnectionStrategy {
        val strategy = DefaultPeersConnectionStrategy(connMan, me)
        strategy.backupConnTimeMax = 102
        strategy.backupConnTimeMin = 100
        strategy.reconnectTimeMax = 92
        strategy.reconnectTimeMin = 90
        return strategy
    }

    @Test
    fun singleConn() {
        testConnectAll(peer2, setOf(peer1), setOf(peer1))
    }

    @Test
    fun threeConns() {
        testConnectAll(peer4, setOf(peer1, peer2, peer3), setOf(peer1, peer2, peer3))
    }

    @Test
    fun noConns() {
        testConnectAll(peer1, setOf(), setOf())
    }

    @Test
    fun onlySingleBackup() {
        testConnectAll(peer1, setOf(peer2), setOf())
    }

    @Test
    fun onlySingleConnAndSingleBackup() {
        testConnectAll(peer2, setOf(peer1, peer3), setOf(peer1))
    }

    fun connectionLost(me: XPeerID, peerIds: Set<XPeerID>, lostpeer: XPeerID, outgoing: Boolean) {
        val strategy = testConnectAll(me, peerIds, setOf())
        reset(connMan)
        whenever(connMan.isPeerConnected(0, lostpeer)).thenReturn(false)
        strategy.connectionLost(0, lostpeer, outgoing)
        Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).untilAsserted {
            verify(connMan).connectChainPeer(0, lostpeer)
        }
    }

    @Test
    fun lostOutgoing() {
        connectionLost(peer1, setOf(peer2, peer3), peer2, true)
    }

    @Test
    fun lostIncoming() {
        connectionLost(peer1, setOf(peer2, peer3), peer3, false)
    }

    @Test
    fun lostNonexisting() {
        // This should actually work the same as if the peer was part of the network
        // because the strategy doesn't know which nodes there are. It just reacts
        // to events. It's up to the caller to discriminate among nodes.
        connectionLost(peer1, setOf(peer2, peer3), peer4, false)
    }

    @Test
    fun lostAlreadyConnected() {
        val strategy = testConnectAll(peer1, setOf(peer2, peer3), setOf())
        reset(connMan)
        whenever(connMan.isPeerConnected(0, peer3)).thenReturn(true)
        strategy.connectionLost(0, peer3, true)
        sleep(200)
        verify(connMan, times(0)).connectChainPeer(0, peer3)
    }
}