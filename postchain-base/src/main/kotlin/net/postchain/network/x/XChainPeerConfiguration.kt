package net.postchain.network.x

import net.postchain.base.PeerCommConfiguration
import net.postchain.network.XPacketDecoder
import net.postchain.network.XPacketEncoder

/* TODO: merge with PeerCommConfiguration */
open class XChainPeerConfiguration(
        val chainID: Long,
        val blockchainRID: ByteArray,
        val commConfiguration: PeerCommConfiguration, // TODO: Rename it
        val packetHandler: XPacketHandler,
        val packetEncoder: XPacketEncoder<*>,
        val packetDecoder: XPacketDecoder<*>
)