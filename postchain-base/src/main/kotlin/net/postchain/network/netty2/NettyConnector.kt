package net.postchain.network.netty2

import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import mu.KLogging
import net.postchain.base.PeerInfo
import net.postchain.core.byteArrayKeyOf
import net.postchain.network.XPacketDecoder
import net.postchain.network.XPacketEncoder
import net.postchain.network.x.XConnector
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPeerConnectionDescriptor
import java.util.concurrent.TimeUnit

class NettyConnector<PacketType>(
        private val eventReceiver: XConnectorEvents
) : XConnector<PacketType> {

    companion object : KLogging()

    private lateinit var server: NettyServer
    private lateinit var eventGroup: EventLoopGroup

    override fun init(peerInfo: PeerInfo, packetDecoder: XPacketDecoder<PacketType>) {
        eventGroup = NioEventLoopGroup(3)

        server = NettyServer(eventGroup).apply {
            setChannelHandler {
                NettyServerPeerConnection(packetDecoder).onConnected { connection, identPacketInfo ->
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

    override fun connectPeer(peerConnectionDescriptor: XPeerConnectionDescriptor, peerInfo: PeerInfo, packetEncoder: XPacketEncoder<PacketType>) {
        try {
            NettyClientPeerConnection(eventGroup, peerInfo, packetEncoder).also { connection ->
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
        eventGroup.shutdownGracefully(2, 2, TimeUnit.SECONDS).sync()
    }
}
