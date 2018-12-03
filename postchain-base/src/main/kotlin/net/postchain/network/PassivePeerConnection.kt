package net.postchain.network

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import kotlin.concurrent.thread

class PassivePeerConnection(
        private val packetConverter: IdentPacketConverter,
        inSocket: Socket,
        val registerConnection: (info: IdentPacketInfo, PeerConnection) -> (ByteArray) -> Unit
) : PeerConnection() {

    lateinit var packetHandler: (ByteArray) -> Unit

    override fun handlePacket(pkt: ByteArray) {
        packetHandler(pkt)
    }

    init {
        socket = inSocket
        thread(name = "PassiveReadLoop-PeerId-TBA") { readLoop(inSocket) }
    }

    private fun writeLoop(socket1: Socket) {
        try {
            val stream = DataOutputStream(socket1.getOutputStream())
            val err = writePacketsWhilePossible(stream)
            if (err != null) {
                logger.debug("closing socket to: ${err.message}")
            }
            socket1.close()
        } catch (e: Exception) {
            logger.error("failed to cleanly close connection to", e)
        }
    }

    fun readLoop(socket1: Socket) {
        try {
            val stream = DataInputStream(socket1.getInputStream())

            val info = packetConverter.parseIdentPacket(readOnePacket(stream))
            Thread.currentThread().name = "PassiveReadLoop-PeerId"
            packetHandler = registerConnection(info, this)

            thread(name = "PassiveWriteLoop-PeerId") { writeLoop(socket1) }

            val err = readPacketsWhilePossible(stream)
            if (err != null) {
                logger.debug("reading packet from stopped: ${err.message}")
                stop()
            }
        } catch (e: Exception) {
            logger.error("readLoop failed", e)
            stop()
        }
    }
}