package net.postchain.network

class BlockchainDataReceiver(private val packets: MutableList<ByteArray>) : BlockchainDataHandler {

    override fun getPacketHandler(peerPubKey: ByteArray): (ByteArray) -> Unit {
        return { packet -> packets.add(packet) }
    }
}