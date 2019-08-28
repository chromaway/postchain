package net.postchain.base

import net.postchain.core.ByteArrayKey
import java.time.Instant

// TODO: Will be replaced by XPeerId
typealias PeerID = ByteArray

open class PeerInfo(val host: String, open val port: Int, val pubKey: ByteArray, val createdAt: Instant? = null, val updatedAt: Instant? = null)

/**
 * Returns [XPeerID] for given [PeerInfo.pubKey] object
 */
fun PeerInfo.peerId() = ByteArrayKey(this.pubKey)

/**
 * Resolves peer by pubKey and returns [PeerInfo]? object
 */
object DefaultPeerResolver {

    fun resolvePeer(peerPubKey: ByteArray, peerInfos: Array<PeerInfo>): PeerInfo? =
            peerInfos.find { it.pubKey.contentEquals(peerPubKey) }

    fun resolvePeerIndex(peerPubKey: ByteArray, peerInfos: Array<PeerInfo>): Int =
            peerInfos.indexOfFirst { it.pubKey.contentEquals(peerPubKey) }
}
