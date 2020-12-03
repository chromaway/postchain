// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.x

import net.postchain.base.BlockchainRid
import net.postchain.base.PeerCommConfiguration

open class XChainPeerConfiguration(
        val chainID: Long,
        val blockchainRID: BlockchainRid,
        val commConfiguration: PeerCommConfiguration,
        val packetHandler: XPacketHandler
)