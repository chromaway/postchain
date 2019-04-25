// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base

class BasePeerCommConfiguration(override val peerInfo: Array<PeerInfo>,
                                override val myIndex: Int,
                                private val cryptoSystem: CryptoSystem,
                                private val privKey: ByteArray
) : PeerCommConfiguration {

    override fun resolvePeer(peerID: ByteArray): PeerInfo? {
        return DefaultPeerResolver.resolvePeer(peerID, peerInfo)
    }

    override fun myPeerInfo(): PeerInfo = peerInfo[myIndex]

    override fun signer(): Signer = cryptoSystem.makeSigner(myPeerInfo().pubKey, privKey)

    override fun verifier(): Verifier = cryptoSystem.makeVerifier()
}