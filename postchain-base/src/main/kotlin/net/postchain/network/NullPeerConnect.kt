package net.postchain.network

import mu.KLogging

class NullPeerConnect() : AbstractPeerConnection {
    companion object : KLogging()

    override fun stop() {/*logger.info("")*/
    }

    override fun sendPacket(b: ByteArray) {/*logger.info(String(b))*/
    }
}