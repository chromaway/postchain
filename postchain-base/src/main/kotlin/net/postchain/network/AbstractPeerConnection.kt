package net.postchain.network

interface AbstractPeerConnection {
    fun stop()
    fun sendPacket(b: ByteArray)
}