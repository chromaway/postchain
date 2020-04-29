// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import mu.KLogging
import net.postchain.common.toHex
import net.postchain.core.ByteArrayKey
import net.postchain.core.UserMistake
import net.postchain.network.x.XPeerID

/**
 * Network nodes can be either signers or nodes that just want to read your data.
 * The purpose of this class is to wrap both these entities
 *
 * The "read-only" nodes do not have to be known by the validator node, but we need some sort of
 * rejection method for these nodes, so they won't ask us too much (DoS attack us).
 *
 * @property myself is this server itself (it can be a signer or a read-only node).
 * @property peerInfoMap keeps track of the OTHER peers (myself not included, if I am a real peer that is)
 * @property readOnlyNodeContacts keeps track of the most recent read-only nodes that contacted us, and how
 *      much they bother us.
 */
class NetworkNodes(
        val myself: PeerInfo,
        private val peerInfoMap: Map<XPeerID, PeerInfo>,
        private val readOnlyNodeContacts: MutableMap<XPeerID, Int>) {

    private var nextTimestamp: Long = 0 // Increases once a day

    companion object: KLogging() {
        const val MAX_DAILY_REQUESTS = 1000 // TODO: What to put here?
        const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000

        fun buildNetworkNodes(peers: Collection<PeerInfo>, myKey: XPeerID): NetworkNodes {
            if (peers.isEmpty()) {
                throw UserMistake("No peers have been configured for the network. Cannot proceed.")
            }
            var me: PeerInfo? = null
            val peerMap = mutableMapOf<XPeerID, PeerInfo>()
            for (peer in peers) {
                val peerId =  peer.peerId()
                if (peerId == myKey) {
                    me = PeerInfo(peer.host, peer.port, myKey)
                } else {
                    peerMap[peerId] = peer
                }
            }
            if (me == null) {
                throw UserMistake("We didn't find our peer ID (${myKey.byteArray.toHex()}) in the list of given peers. Check the configuration for the node.")
            } else {
                return NetworkNodes(me, peerMap.toMap(), mutableMapOf())
            }
        }

        // Only for testing
        fun buildNetworkNodesDummy(): NetworkNodes {
            return NetworkNodes(PeerInfo("abc", 1, byteArrayOf(1)), mapOf(), mutableMapOf())
        }
    }

    fun hasPeers(): Boolean {
        return !peerInfoMap.isEmpty()
    }

    operator fun get(key: XPeerID): PeerInfo? = peerInfoMap[key]
    operator fun get(key: ByteArray): PeerInfo? = peerInfoMap[ByteArrayKey(key)]

    /**
     * Will run an action once for all peers. The trick for connections is to not make it happen twice.
     *
     * @param filterFun is the function that picks the peers who we should do the action on.
     *                    (The reason we filter is that it is enough to have only ONE peer in a connection do the action)
     * @param action is the action to perform
     */
    fun filterAndRunActionOnPeers(filterFun: (Map<XPeerID, PeerInfo>, myKey: XPeerID) -> Set<PeerInfo>, action: (PeerInfo) -> Unit) {
        val filtered = filterFun(peerInfoMap, XPeerID(myself.pubKey))
        filtered.forEach(action)
    }


    /**
     * Call this method before serving a read-only node
     *
     * @return true if the node is not bothering us too much.
     */
    @Synchronized
    fun isNodeBehavingWell(peerId: XPeerID, now: Long): Boolean {

        if (now > nextTimestamp) {
            val totalCalls = readOnlyNodeContacts.values.sum()
            logger.info("------------------------------------")
            logger.info("Clearing the read-only node overuse counter. " +
                    "Number of read-only nodes in contact since yesterday: $readOnlyNodeContacts.size. " +
                    "Total calls from read-only nodes: $totalCalls")
            logger.info("------------------------------------")
            nextTimestamp = now + DAY_IN_MILLIS
            readOnlyNodeContacts.clear()
        }

        val foundSigner = peerInfoMap[peerId]
        if (foundSigner != null) {
            // Do nothing
            // TODO: Only read-only nodes can be shut out currently (don't know what the limit should be for signers)
        } else {
            val foundHits = readOnlyNodeContacts[peerId]
            if (foundHits == null) {
                // Add it
                readOnlyNodeContacts[peerId] = 1
            } else {
                readOnlyNodeContacts[peerId] = foundHits + 1
                if (foundHits > MAX_DAILY_REQUESTS) {
                    if (foundHits == MAX_DAILY_REQUESTS + 1) {
                        logger.debug("Blocking read-only node with ID: ${peerId.byteArray.toHex()} for a day ")
                    }
                    return false
                }
            }
        }

        return true
    }
}