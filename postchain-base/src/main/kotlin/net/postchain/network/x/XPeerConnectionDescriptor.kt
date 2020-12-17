// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import net.postchain.base.BlockchainRid
import net.postchain.core.byteArrayKeyOf
import net.postchain.network.IdentPacketInfo

enum class direction {
    INCOMING, OUTGOING
}

class XPeerConnectionDescriptor(
        val peerId: XPeerID,
        val blockchainRID: BlockchainRid,
        val sessionKey: ByteArray? = null,
        val dir: direction = direction.OUTGOING
) {

    companion object Factory {

        fun createFromIdentPacketInfo(identPacketInfo: IdentPacketInfo): XPeerConnectionDescriptor {
            return XPeerConnectionDescriptor(
                    identPacketInfo.peerID.byteArrayKeyOf(),
                    identPacketInfo.blockchainRID, identPacketInfo.sessionKey, direction.INCOMING)
        }

    }

    fun isOutgoing(): Boolean {
        return dir == direction.OUTGOING
    }
}
