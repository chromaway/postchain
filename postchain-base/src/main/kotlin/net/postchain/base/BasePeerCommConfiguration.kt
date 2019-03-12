// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base

class BasePeerCommConfiguration(override val peerInfo: Array<PeerInfo>,
                                override val blockchainRID: ByteArray,
                                override val myIndex: Int,
                                private val cryptoSystem: CryptoSystem,
                                private val privKey: ByteArray
) : PeerCommConfiguration {

    override fun resolvePeer(peerID: ByteArray): PeerInfo? {
        return peerInfo.find { it.pubKey.contentEquals(peerID) }
    }

    override fun sigMaker(): SigMaker {
        return cryptoSystem.buildSigMaker(peerInfo[myIndex].pubKey, privKey)
    }

    override fun verifier(): Verifier {
        return cryptoSystem.makeVerifier()
    }
}