package net.postchain.base

interface PeerResolver {
    fun resolvePeer(peerID: PeerID): PeerInfo?
}

interface PeerCommConfiguration : PeerResolver {
    val peerInfo: Array<PeerInfo>
    val myIndex: Int
    val blockchainRID: ByteArray
    fun sigMaker(): SigMaker
    fun verifier(): Verifier
}
