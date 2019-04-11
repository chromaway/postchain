package net.postchain.base

import net.postchain.core.ByteArrayKey
import java.util.concurrent.CountDownLatch

// TODO: Will be replaced by XPeerId
typealias PeerID = ByteArray

open class PeerInfo(val host: String, open val port: Int, val pubKey: ByteArray)

@Deprecated("Use {@code zero} port to obtain dynamic/ephemeral port")
class DynamicPortPeerInfo(host: String, pubKey: ByteArray, privateKey: ByteArray? = null) : PeerInfo(host, 0, pubKey) {

    private val latch = CountDownLatch(1)
    private var assignedPortNumber = 0

    override val port: Int
        get() {
            latch.await()
            return assignedPortNumber
        }

    fun portAssigned(port: Int) {
        assignedPortNumber = port
        latch.countDown()
    }
}

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
