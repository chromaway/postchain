package net.postchain.network.netty2

import mu.KLogging
import net.postchain.base.PeerInfo
import net.postchain.core.byteArrayKeyOf
import net.postchain.network.PacketConverter
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnectionDescriptor

class NettyConnector<PC : PacketConverter<*>>(
        val packetConverter: PC,
        val eventReceiver: XConnectorEvents
) : XConnector {

    companion object : KLogging()

    private lateinit var server: NettyServer

    override fun init(peerInfo: PeerInfo) {
        server = NettyServer().apply {
            setChannelHandler {
                NettyServerPeerConnection(packetConverter).onConnected { connection, identPacketInfo ->
                    val descriptor = XPeerConnectionDescriptor(
                            identPacketInfo.peerID.byteArrayKeyOf(),
                            identPacketInfo.blockchainRID.byteArrayKeyOf())
                    eventReceiver.onPeerConnected(descriptor, connection)
                            ?.also { connection.accept(it) }
//                            ?: connection.close()
                }
            }
            run(peerInfo.port)
        }
    }

    override fun connectPeer(peerConnectionDescriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
        try {
            NettyClientPeerConnection(peerInfo, packetConverter).also { connection ->
                connection.open(
                        onConnected = {
                            eventReceiver.onPeerConnected(peerConnectionDescriptor, connection)
                                    ?.also { connection.accept(it) }
//                                    ?: connection.close()
                        },
                        onDisconnected = {
                            eventReceiver.onPeerDisconnected(peerConnectionDescriptor)
                        }
                )
            }
        } catch (e: Exception) {
            logger.error { e.message }
            eventReceiver.onPeerDisconnected(peerConnectionDescriptor) // TODO: [et]: Maybe create different event receiver.
        }
    }

    override fun shutdown() {
        server.shutdown()
    }
}
