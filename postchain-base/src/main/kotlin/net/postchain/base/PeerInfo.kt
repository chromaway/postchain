package net.postchain.base

import net.postchain.core.ByteArrayKey
import net.postchain.network.x.XPeerID
import java.time.Instant

// TODO: Will be replaced by XPeerId
typealias PeerID = ByteArray


/**
 * A "peer" is either
 * 1. a block producer/signer who takes part in the consensus discussion.
 * 2. a read-only node
 */
open class PeerInfo(val host: String, val port: Int, val pubKey: ByteArray, val timestamp: Instant? = null) {

    constructor(host: String, port: Int, pubKey: ByteArrayKey, timestamp: Instant? = null) : this(host, port, pubKey.byteArray, timestamp)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PeerInfo

        if (host != other.host) return false
        if (port != other.port) return false
        if (!pubKey.contentEquals(other.pubKey)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port
        result = 31 * result + pubKey.contentHashCode()
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        return result
    }
}

/**
 * Returns [XPeerID] for given [PeerInfo.pubKey] object
 */
fun PeerInfo.peerId() = ByteArrayKey(this.pubKey)

