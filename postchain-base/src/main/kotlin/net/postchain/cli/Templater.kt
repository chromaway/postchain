// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import net.postchain.base.PeerInfo
import net.postchain.common.toHex

object Templater {

    object PeerInfoTemplater {
        fun renderPeerInfo(index: Int, peerInfo: PeerInfo): String {
            return "  ${index + 1}:\t${peerInfo.host}:${peerInfo.port}\t${peerInfo.pubKey.toHex()}"
        }
    }
}