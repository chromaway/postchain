package net.postchain.network.x

import net.postchain.base.PeerCommConfiguration
import net.postchain.network.IdentPacketConverter

/* TODO: merge with PeerCommConfiguration */
open class XChainPeerConfiguration(
        val chainID: Long,
        val commConfiguration: PeerCommConfiguration,
        val packetHandler: XPacketHandler,
        /* this implies that conn manager handles authentication in
        a particular way and puts the burden of authentication into higher
        level protocols. this is not good, and in future we will make
        conn manager fully responsible for authentication process
        */
        val identPacketConverter: IdentPacketConverter
)