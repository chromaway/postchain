package net.postchain.base

interface PeerResolver {
    fun resolvePeer(peerID: PeerID): PeerInfo?
}

interface PeerCommConfiguration : PeerResolver {
    val peerInfo: Array<PeerInfo>
    val myIndex: Int
    val blockchainRID: ByteArray
    fun signer(): Signer
    fun verifier(): Verifier
    fun othersPeerInfo(): List<PeerInfo>
}
