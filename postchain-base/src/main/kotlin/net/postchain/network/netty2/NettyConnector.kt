package net.postchain.network.netty2

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

    private lateinit var server: NettyServer

    override fun init(peerInfo: PeerInfo) {
        server = NettyServer().apply {
            setChannelHandler {
                NettyServerPeerConnection(packetConverter).onConnected { connection, identPacketInfo ->
                    val descriptor = XPeerConnectionDescriptor(
                            identPacketInfo.peerID.byteArrayKeyOf(),
                            byteArrayOf().byteArrayKeyOf()) // TODO: Fix this stub
                    eventReceiver.onPeerConnected(descriptor, connection)?.also(connection::accept)
                }
            }
            run(peerInfo.port)
        }
    }

    override fun connectPeer(peerConnectionDescriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo) {
        NettyClientPeerConnection(peerInfo, packetConverter).also { connection ->
            connection.open {
                eventReceiver.onPeerConnected(peerConnectionDescriptor, connection)
                        ?.also(connection::accept)
            }
        }
    }

    override fun shutdown() {
        server.shutdown()
    }
}
