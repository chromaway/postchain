package net.postchain.devtools

import net.postchain.common.toHex
import net.postchain.network.x.XPeerID

object PeerNameHelper {

    fun peerName(pubKey: String): String = pubKey.run {
        "${take(2)}:${takeLast(2)}"
    }

    fun peerName(pubKey: ByteArray): String = pubKey.toHex().run {
        "${take(2)}:${takeLast(2)}"
    }

    fun peerName(peerId: XPeerID): String = peerName(peerId.toString())

}