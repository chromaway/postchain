package net.postchain.network.netty2

import mu.KLogging
import net.postchain.base.PeerInfo
import net.postchain.network.XPacketDecoder
import net.postchain.network.XPacketEncoder
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnectionDescriptor

class NettyConnector<PacketType>(
        private val eventReceiver: XConnectorEvents
) : XConnector<PacketType> {

    companion object : KLogging()

    private lateinit var server: NettyServer

    override fun init(peerInfo: PeerInfo, packetDecoder: XPacketDecoder<PacketType>) {
        server = NettyServer().apply {
            setChannelHandler {
                NettyServerPeerConnection(packetDecoder)
                        .onConnected { descriptor, connection ->
                            eventReceiver.onPeerConnected(descriptor, connection)
                                    ?.also { connection.accept(it) }
                        }
                        .onDisconnected { descriptor, connection ->
                            eventReceiver.onPeerDisconnected(descriptor, connection)
                        }
            }

            run(peerInfo.port)
        }
    }

    override fun connectPeer(
            peerConnectionDescriptor: XPeerConnectionDescriptor,
            peerInfo: PeerInfo,
            packetEncoder: XPacketEncoder<PacketType>
    ) {
        with(NettyClientPeerConnection(peerInfo, packetEncoder)) {
            try {
                open(
                        onConnected = {
                            eventReceiver.onPeerConnected(peerConnectionDescriptor, this)
                                    ?.also { this.accept(it) }
                        },
                        onDisconnected = {
                            eventReceiver.onPeerDisconnected(peerConnectionDescriptor, this)
                        })
            } catch (e: Exception) {
                logger.error { e.message }
                eventReceiver.onPeerDisconnected(peerConnectionDescriptor, this) // TODO: [et]: Maybe create different event receiver.
            }
        }
    }

    override fun shutdown() {
        server.shutdown()
    }
}
