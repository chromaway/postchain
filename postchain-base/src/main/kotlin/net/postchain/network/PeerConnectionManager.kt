package net.postchain.network

import mu.KLogging
import net.postchain.base.PeerInfo
import net.postchain.common.toHex
import net.postchain.core.ByteArrayKey
import net.postchain.core.Shutdownable
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

interface PeerConnectionManagerInterface<PacketType> {
    fun sendPacket(packet: OutboundPacket<PacketType>)
    fun connectPeer(peer: PeerInfo, packetConverter: IdentPacketConverter, packetHandler: (ByteArray) -> Unit): AbstractPeerConnection
    fun registerBlockchain(blockchainRID: ByteArray, handler: BlockchainDataHandler)
}

class PeerConnectionManager<PacketType>(
        myPeerInfo: PeerInfo,
        val packetConverter: PacketConverter<PacketType>
) : PeerConnectionManagerInterface<PacketType>, Shutdownable {

    val connections = mutableListOf<AbstractPeerConnection>() // TODO: Remove it and use `peerConnections`
    val peerConnections = Collections.synchronizedMap(mutableMapOf<ByteArrayKey, AbstractPeerConnection>()) // connection for peerID
    @Volatile
    private var keepGoing: Boolean = true
    private val encoderThread: Thread
    private val connAcceptor: PeerConnectionAcceptor
    private val blockchains = mutableMapOf<ByteArrayKey, BlockchainDataHandler>()
    val outboundPackets = LinkedBlockingQueue<OutboundPacket<PacketType>>(MAX_QUEUED_PACKETS)

    companion object : KLogging()

    init {
        encoderThread = thread(name = "encoderLoop") { encoderLoop() }

        val registerConn = { info: IdentPacketInfo, conn: PeerConnection ->
            val bh = blockchains[ByteArrayKey(info.blockchainRID)]
            if (bh != null) {
                connections.add(conn)
                peerConnections[ByteArrayKey(info.peerID)] = conn
                logger.info("Registering incoming connection ")
                bh.getPacketHandler(info.peerID)
            } else {
                logger.warn("Got packet with unknown blockchainRID:${info.blockchainRID.toHex()}, skipping")
                throw Error("Blockchain not found")
            }
        }

        connAcceptor = PeerConnectionAcceptor(
                myPeerInfo, packetConverter, registerConn)
    }

    private fun encoderLoop() {
        while (keepGoing) {
            try {
                val pkt = outboundPackets.take()
                if (!keepGoing) return
                val data = packetConverter.encodePacket(pkt.packet)

                for (r in pkt.recipients) {
                    val conn = peerConnections[r]
                    if (conn != null) {
                        conn.sendPacket(data)
                    }
                }
            } catch (e: InterruptedException) {
                logger.debug { "interrupted while taking next outbound packet" }
            } catch (e: Exception) {
                logger.debug("Exception in encoderLoop", e)
            }
        }
    }

    override fun sendPacket(packet: OutboundPacket<PacketType>) {
        outboundPackets.add(packet)
    }

    override fun connectPeer(
            peer: PeerInfo,
            packetConverter: IdentPacketConverter,
            packetHandler: (ByteArray) -> Unit
    ): AbstractPeerConnection {

        return ActivePeerConnection(peer, packetConverter, packetHandler)
                .apply { start() }
                .also {
                    connections.add(it)
                    peerConnections[ByteArrayKey(peer.pubKey)] = it
                }
    }

    override fun registerBlockchain(blockchainRID: ByteArray, handler: BlockchainDataHandler) {
        blockchains[ByteArrayKey(blockchainRID)] = handler
    }

    override fun shutdown() {
        keepGoing = false
        connAcceptor.stop()
        connections.forEach { it.stop() }
        encoderThread.interrupt()
    }
}