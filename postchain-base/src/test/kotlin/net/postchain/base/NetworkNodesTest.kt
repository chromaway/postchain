// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.network.x.DefaultPeersConnectionStrategy
import net.postchain.network.x.XPeerID
import org.junit.Test

class NetworkNodesTest {

    val peerKey1 = "11".hexStringToByteArray()
    val peerKey2 = "22".hexStringToByteArray()
    val peerKey3 = "33".hexStringToByteArray()
    val peerKey4 = "44".hexStringToByteArray()

    val peer1 = PeerInfo("12", 12, peerKey1)
    val peer2 = PeerInfo("12", 13, peerKey2)
    val peer3 = PeerInfo("12", 14, peerKey3)
    val peer4 = PeerInfo("12", 15, peerKey4)


    @Test
    fun testFiltering_all() {

        val myKey = "36".hexStringToByteArray()
        val me = PeerInfo("12", 11, myKey)
        val nn = NetworkNodes.buildNetworkNodes(setOf(peer2, peer4, me, peer1, peer3 ), XPeerID(myKey))

        // Stupid filter function allowing everything to pass
        val filterAllFun: (Map<XPeerID, PeerInfo>, XPeerID) -> Set<PeerInfo> = {
            theMap: Map<XPeerID, PeerInfo>, y: XPeerID ->
            val r = theMap.values.toSet()
            r
        }


        var strs = mutableSetOf<String>()
        val action = {
            p: PeerInfo ->
            val strKey = p.pubKey.toHex()
            strs.add(strKey)
            Unit
        }

        nn.filterAndRunActionOnPeers(filterAllFun, action)

        assert(setOf("11", "22", "33", "44").containsAll(strs))

    }


    @Test
    fun testFiltering_half() {

        val myKey = "26".hexStringToByteArray()
        val me = PeerInfo("12", 11, myKey)
        val nn = NetworkNodes.buildNetworkNodes(setOf(peer2, peer4, me, peer1, peer3 ), XPeerID(myKey))

        var strs = mutableSetOf<String>()
        val action = {
            p: PeerInfo ->
            val strKey = p.pubKey.toHex()
            strs.add(strKey)
            Unit
        }

        nn.filterAndRunActionOnPeers(DefaultPeersConnectionStrategy::getPeersThatShouldDoAction, action)

        assert(setOf("11", "22").containsAll(strs))

    }

    @Test
    fun testFiltering_75pct() {

        val myKey = "36".hexStringToByteArray()
        val me = PeerInfo("12", 11, myKey)
        val nn = NetworkNodes.buildNetworkNodes(setOf(peer2, peer4, me, peer1, peer3 ), XPeerID(myKey))

        var strs = mutableSetOf<String>()
        val action = {
            p: PeerInfo ->
            val strKey = p.pubKey.toHex()
            strs.add(strKey)
            Unit
        }

        nn.filterAndRunActionOnPeers(DefaultPeersConnectionStrategy::getPeersThatShouldDoAction, action)

        assert(sortedSetOf("11", "22", "33").containsAll(strs))

    }
}