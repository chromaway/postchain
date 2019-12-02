package net.postchain.network.x

import net.postchain.base.BlockchainRid
import net.postchain.core.ByteArrayKey
import net.postchain.core.byteArrayKeyOf
import net.postchain.network.IdentPacketInfo

class XPeerConnectionDescriptor(
        val peerId: XPeerID,
        val blockchainRID: BlockchainRid,
        val sessionKey: ByteArray? = null
) {

    companion object Factory {

        fun createFromIdentPacketInfo(identPacketInfo: IdentPacketInfo): XPeerConnectionDescriptor {
            return XPeerConnectionDescriptor(
                    identPacketInfo.peerID.byteArrayKeyOf(),
                    identPacketInfo.blockchainRID)
        }

    }
}
