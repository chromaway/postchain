package net.postchain.base

interface PeerResolver {
    fun resolvePeer(peerID: PeerID): PeerInfo?
}

interface PeerCommConfiguration : PeerResolver {
    val peerInfo: Array<PeerInfo>
    val myIndex: Int
    val blockchainRID: ByteArray // TODO: [et]: Remove this
    fun myPeerInfo(): PeerInfo
    fun signer(): Signer
    fun verifier(): Verifier
}
