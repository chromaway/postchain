package net.postchain.network

import mu.KLogging
import net.postchain.base.PeerInfo
import net.postchain.common.toHex
import net.postchain.core.ByteArrayKey
import net.postchain.core.Shutdownable
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class PeerConnectionManager<PacketType>(
        myPeerInfo: PeerInfo,
        val packetConverter: PacketConverter<PacketType>
) : Shutdownable {

    val connections = mutableListOf<AbstractPeerConnection>()
    val peerConnections = Collections.synchronizedMap(mutableMapOf<ByteArrayKey, AbstractPeerConnection>()) // connection for peerID
    @Volatile
    private var keepGoing: Boolean = true
    private val encoderThread: Thread
    private val connAcceptor: PeerConnectionAcceptor

    private val blockchains = mutableMapOf<ByteArrayKey, BlockchainDataHandler>()

    val outboundPackets = LinkedBlockingQueue<OutboundPacket<PacketType>>(MAX_QUEUED_PACKETS)

    companion object : KLogging()

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

    fun sendPacket(packet: OutboundPacket<PacketType>) {
        outboundPackets.add(packet)
    }

    fun connectPeer(peer: PeerInfo, packetConverter: IdentPacketConverter, packetHandler: (ByteArray) -> Unit): AbstractPeerConnection {
        val conn = ActivePeerConnection(peer,
                packetConverter,
                packetHandler
        )
        conn.start()
        connections.add(conn)
        peerConnections[ByteArrayKey(peer.pubKey)] = conn
        return conn
    }

    fun stop() {
        keepGoing = false
        connAcceptor.stop()
        for (c in connections) c.stop()
        encoderThread.interrupt()
    }

    override fun shutdown() {
        stop()
    }

    fun registerBlockchain(blockchainRID: ByteArray, h: BlockchainDataHandler) {
        blockchains[ByteArrayKey(blockchainRID)] = h
    }

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
                myPeerInfo,
                packetConverter, registerConn
        )
    }
}