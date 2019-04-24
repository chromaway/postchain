package net.postchain.network

@Deprecated("Deprecated after Netty2")
interface BlockchainDataHandler {
    fun getPacketHandler(peerPubKey: ByteArray): (ByteArray) -> Unit
}