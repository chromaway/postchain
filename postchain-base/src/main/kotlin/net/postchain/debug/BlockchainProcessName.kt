// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.debug

import net.postchain.base.BlockchainRid
import net.postchain.devtools.PeerNameHelper

data class BlockchainProcessName(val pubKey: String, val blockchainRID: BlockchainRid) {

    override fun toString(): String = "[${PeerNameHelper.peerName(pubKey)}]/[${blockchainRID.toShortHex()}]"

}