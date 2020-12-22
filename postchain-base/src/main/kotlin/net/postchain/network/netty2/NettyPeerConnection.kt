package net.postchain.network.netty2

import io.netty.channel.ChannelInboundHandlerAdapter
import mu.KLogging
import net.postchain.network.x.XPeerConnection
import net.postchain.network.x.XPeerID

abstract class NettyPeerConnection: ChannelInboundHandlerAdapter(), XPeerConnection {
    companion object: KLogging()
    fun handleSafely(peerId: XPeerID?, handler: () -> Unit) {
        try {
            handler()
        } catch (e: Exception) {
            logger.error("Error when receiving message from peer ${peerId}", e)
        }
    }
}