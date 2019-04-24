package net.postchain.network

import net.postchain.base.PeerInfo
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread

@Deprecated("Deprecated after Netty2")
class ActivePeerConnection(
        val peer: PeerInfo,
        val packetConverter: IdentPacketConverter,
        val packetHandler: (ByteArray) -> Unit
) : PeerConnection() {

    private val awaitConnection = CyclicBarrier(2)

    override fun handlePacket(pkt: ByteArray) {
        packetHandler(pkt)
    }

    private fun writeLoop() {
        while (keepGoing) {
            try {
                if (socket != null && !(socket!!.isClosed)) socket!!.close()
                socket = Socket(peer.host, peer.port)
                // writer loop sets up a serverSocket then waits for read loop to sync
                // if exception is thrown when connecting, read loop will just wait for the next cycle
                awaitConnection.await()
                val socket1 = socket ?: throw Exception("No connection")
                val stream = DataOutputStream(socket1.getOutputStream())
                writeOnePacket(stream, packetConverter.makeIdentPacket(peer.pubKey)) // write Ident packet
                val err = writePacketsWhilePossible(stream)
                if (err != null) {
                    logger.debug(" sending packet to  failed: ${err.message}")
                }
                socket1.close()
            } catch (e: Exception) {
                logger.debug(" disconnected from : ${e.message}")
                Thread.sleep(2500)
            }
        }
    }

    private fun readLoop() {
        while (keepGoing) {
            try {
                awaitConnection.await()
                val socket1 = socket ?: throw Exception("No connection")
                val err = readPacketsWhilePossible(DataInputStream(socket1.getInputStream()))
                if (err != null) {
                    logger.debug("reading packet from  failed: ${err.message}")
                }
                socket1.close()
            } catch (e: Exception) {
                logger.debug("readLoop for failed. Will retry. ${e.message}")
                Thread.sleep(2500)
            }
        }
    }

    fun start() {
        thread(name = "-ActiveWriteLoop-PeerId-") { writeLoop() }
        thread(name = "-ActiveReadLoop-PeerId-") { readLoop() }
    }
}