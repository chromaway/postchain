package net.postchain.base

import mu.KLogging
import net.postchain.core.ByteArrayKey
import net.postchain.network.x.XPeerID

// TODO: Will be replaced by XPeerId
typealias PeerID = ByteArray


/**
 * A "peer" is either
 * 1. a block producer/signer who takes part in the consensus discussion.
 * 2. a read-only node
 */
open class PeerInfo(val host: String, val port: Int, val pubKey: ByteArray) {

    constructor(host: String, port: Int, pubKey: ByteArrayKey): this(host, port, pubKey.byteArray)
}

/**
 * Returns [XPeerID] for given [PeerInfo.pubKey] object
 */
fun PeerInfo.peerId() = ByteArrayKey(this.pubKey)


