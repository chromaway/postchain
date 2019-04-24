package net.postchain.network

import mu.KLogging
import net.postchain.base.DynamicPortPeerInfo
import net.postchain.base.PeerInfo
import java.net.ServerSocket
import kotlin.concurrent.thread

@Deprecated("Deprecated after Netty2")
class PeerConnectionAcceptor(
        peer: PeerInfo,
        private val identPacketConverter: IdentPacketConverter,
        private val registerConnection: (IdentPacketInfo, PeerConnection) -> (ByteArray) -> Unit
) {
    private val serverSocket: ServerSocket
    @Volatile
    var keepGoing = true

    companion object : KLogging()

    init {
        if (peer is DynamicPortPeerInfo) {
            serverSocket = ServerSocket(0)
            peer.portAssigned(serverSocket.localPort)
        } else {
            serverSocket = ServerSocket(peer.port)
        }
        logger.info("Starting server on port ${peer.port} done")
        thread(name = "-acceptLoop") { acceptLoop() }
    }

    private fun acceptLoop() {
        try {
            while (keepGoing) {
                val socket = serverSocket.accept()
                logger.info("accept socket")
                PassivePeerConnection(
                        identPacketConverter,
                        socket,
                        registerConnection
                )
            }
        } catch (e: Exception) {
            logger.error("exiting accept loop")
        }
    }

    fun stop() {
        keepGoing = false
        serverSocket.close()
    }
}