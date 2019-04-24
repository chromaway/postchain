package net.postchain.network

@Deprecated("Deprecated after Netty2")
class BlockchainDataReceiver(private val packets: MutableList<ByteArray>) : BlockchainDataHandler {

    override fun getPacketHandler(peerPubKey: ByteArray): (ByteArray) -> Unit {
        return { packet -> packets.add(packet) }
    }
}