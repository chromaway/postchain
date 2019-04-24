package net.postchain.network

@Deprecated("Deprecated after Netty2")
interface AbstractPeerConnection {
    fun stop()
    fun sendPacket(b: ByteArray)
}