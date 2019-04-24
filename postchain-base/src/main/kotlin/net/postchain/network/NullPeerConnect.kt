package net.postchain.network

import mu.KLogging

@Deprecated("Deprecated after Netty2")
class NullPeerConnect() : AbstractPeerConnection {
    companion object : KLogging()

    override fun stop() {/*logger.info("")*/
    }

    override fun sendPacket(b: ByteArray) {/*logger.info(String(b))*/
    }
}