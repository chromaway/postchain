// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.netty2

import mu.KLogging
import net.postchain.base.PeerInfo
import net.postchain.base.peerId
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
                        .onConnected { connection ->
                            eventReceiver.onPeerConnected(connection)
                                    ?.also { connection.accept(it) }
                        }
                        .onDisconnected { connection ->
                            eventReceiver.onPeerDisconnected(connection)
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
        with(NettyClientPeerConnection(peerInfo, packetEncoder, peerConnectionDescriptor)) {
            try {
                open(
                        onConnected = {
                            eventReceiver.onPeerConnected(this)
                                    ?.also { this.accept(it) }
                        },
                        onDisconnected = {
                            eventReceiver.onPeerDisconnected(this)
                        })
            } catch (e: Exception) {
                logger.error("Netty Connect Failed, ${peerConnectionDescriptor.loggingPrefix(peerInfo.peerId())}, with message: ${e.message}")
                eventReceiver.onPeerDisconnected(this) // TODO: [et]: Maybe create different event receiver.
            }
        }
    }

    override fun shutdown() {
        server.shutdown()
    }
}
