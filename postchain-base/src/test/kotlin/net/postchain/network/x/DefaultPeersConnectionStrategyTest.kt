// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import com.nhaarman.mockitokotlin2.*
import net.postchain.common.hexStringToByteArray
import org.awaitility.Awaitility
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class DefaultPeersConnectionStrategyTest {

    val peer1 = XPeerID("111111".hexStringToByteArray())
    val peer2 = XPeerID("222222".hexStringToByteArray())
    val peer3 = XPeerID("333333".hexStringToByteArray())
    val peer4 = XPeerID("444444".hexStringToByteArray())
    val peerCaptor = argumentCaptor<XPeerID>()
    val peerCaptor2 = argumentCaptor<XPeerID>()
    val chainCaptor = argumentCaptor<Long>()
    val connMan : XConnectionManager = mock()

    fun testConnectAll(me: XPeerID, peerIds: Set<XPeerID>, expectedConns: Set<XPeerID>) {
        val strategy = DefaultPeersConnectionStrategy(connMan, me)
        strategy.backupConnTimeMax = 102
        strategy.backupConnTimeMin = 100
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
}