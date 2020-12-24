// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

interface PeerResolver {
    fun resolvePeer(peerID: PeerID): PeerInfo?
}

interface PeerCommConfiguration : PeerResolver {
    val networkNodes: NetworkNodes
    val pubKey: ByteArray
    fun myPeerInfo(): PeerInfo
    fun sigMaker(): SigMaker
    fun verifier(): Verifier
}
