// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base

import net.postchain.core.UserMistake

class BasePeerCommConfiguration(override val peerInfo: Array<PeerInfo>,
                                private val cryptoSystem: CryptoSystem,
                                private val privKey: ByteArray,
                                override val pubKey: ByteArray
) : PeerCommConfiguration {

    override fun resolvePeer(peerID: ByteArray): PeerInfo? {
        return DefaultPeerResolver.resolvePeer(peerID, peerInfo)
    }

    override fun myPeerInfo(): PeerInfo = resolvePeer(pubKey)
            ?: throw UserMistake("PubKey is not inside peerInfo array")

    override fun signer(): Signer = cryptoSystem.makeSigner(pubKey, privKey)

    override fun verifier(): Verifier = cryptoSystem.makeVerifier()
}