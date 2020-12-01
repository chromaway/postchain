package net.postchain.network.netty2

import io.netty.channel.ChannelInboundHandlerAdapter
import net.postchain.core.BadDataMistake
import net.postchain.core.BadDataType
import net.postchain.network.x.XPeerConnection
import net.postchain.network.x.XPeerID

abstract class NettyPeerConnection: ChannelInboundHandlerAdapter(), XPeerConnection {
    fun handleSafely(peerId: XPeerID?, handler: () -> Unit) {
        try {
            handler()
        } catch (e: BadDataMistake) {
            if (e.type == BadDataType.BAD_MESSAGE) {
                NettyServerPeerConnection.logger.info("Bad message received from peer ${peerId}: ${e.message}")
            } else {
                NettyServerPeerConnection.logger.error("Error when receiving message from peer ${peerId}", e)
            }
        } catch (e: Exception) {
            NettyServerPeerConnection.logger.error("Error when receiving message from peer ${peerId}", e)
        }
    }
}