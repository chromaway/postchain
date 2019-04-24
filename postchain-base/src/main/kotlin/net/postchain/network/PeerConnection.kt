package net.postchain.network

import mu.KLogging
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue

/*
    PeerConnection implements a simple protocol to send data packets (ByteArray:s)
    over a TCP/IP connection. The protocol doesn't care about meaning of packets,
    only their size.

    While the protocol is very generic, interface is designed to work with ebft:

    1. Connection is unreliable, no attempts are made to retransmit messages,
    they might be dropped at any time. We assume that this is handled at application level.
    2. Every peer has an integer ID meaning of which is understood only on higher level.
    3. sendPacket is non-blocking and doesn't guarantee delivery

    The protocol: each packet is preceded by 4-byte length as Java Int (big-endian).
    Thus max packet size is around 2 GB.
    The first packet sent by a connecting party is an identification packet.
    The first packet received by accepting party is an identification packet.

*/

@Deprecated("Deprecated after Netty2")
abstract class PeerConnection : AbstractPeerConnection {
    @Volatile
    protected var keepGoing: Boolean = true
    @Volatile
    var socket: Socket? = null
    private val outboundPackets = LinkedBlockingQueue<ByteArray>(MAX_QUEUED_PACKETS)

    companion object : KLogging()

    abstract fun handlePacket(pkt: ByteArray)

    protected fun readOnePacket(dataStream: DataInputStream): ByteArray {
        val packetSize = dataStream.readInt()
        if (packetSize > MAX_PAYLOAD_SIZE)
            throw Error("Packet too large")
        val bytes = ByteArray(packetSize)
        dataStream.readFully(bytes)
        logger.trace("Packet received. Length: ${bytes.size}")
        return bytes
    }

    protected fun readPacketsWhilePossible(dataStream: DataInputStream): Exception? {
        try {
            while (keepGoing) {
                val bytes = readOnePacket(dataStream)
                if (bytes.isEmpty()) {
                    // This is a special packet sent when other side is closing
                    // ignore
                    continue
                }
                handlePacket(bytes)
            }
        } catch (e: Exception) {
            outboundPackets.put(byteArrayOf())
            return e
        }
        return null
    }

    protected fun writeOnePacket(dataStream: DataOutputStream, bytes: ByteArray) {
        dataStream.writeInt(bytes.size)
        dataStream.write(bytes)
        logger.trace("Packet sent: ${bytes.size}")
    }

    protected fun writePacketsWhilePossible(dataStream: DataOutputStream): Exception? {
        try {
            while (keepGoing) {
                val bytes = outboundPackets.take()
                if (!keepGoing) return null
                writeOnePacket(dataStream, bytes)
            }
        } catch (e: Exception) {
            return e
        }
        return null
    }

    @Synchronized
    override fun stop() {
        keepGoing = false
        outboundPackets.put(byteArrayOf())
        socket?.close()
    }

    override fun sendPacket(b: ByteArray) {
        if (!keepGoing) return
        if (outboundPackets.size >= MAX_QUEUED_PACKETS) {
            outboundPackets.poll()
        }
        outboundPackets.put(b)
    }
}