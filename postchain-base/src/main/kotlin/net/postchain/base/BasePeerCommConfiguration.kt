// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import net.postchain.core.ByteArrayKey

class BasePeerCommConfiguration(override val networkNodes: NetworkNodes,
                                private val cryptoSystem: CryptoSystem,
                                private val privKey: ByteArray,
                                override val pubKey: ByteArray
) : PeerCommConfiguration {

    companion object {
        // Used in tests only
        fun build(peers: Array<PeerInfo>,
                  cryptoSystem: CryptoSystem,
                  privKey: ByteArray,
                  pubKey: ByteArray
        ): BasePeerCommConfiguration {
            return build(peers.toSet(), cryptoSystem, privKey, pubKey)
        }

        fun build(peers: Collection<PeerInfo>,
                  cryptoSystem: CryptoSystem,
                  privKey: ByteArray,
                  pubKey: ByteArray
        ): BasePeerCommConfiguration {
            val nn = NetworkNodes.buildNetworkNodes(peers, ByteArrayKey(pubKey))
            return BasePeerCommConfiguration(nn, cryptoSystem, privKey, pubKey)
        }
    }


    override fun resolvePeer(peerID: ByteArray): PeerInfo? {
        return networkNodes[peerID]
    }

    override fun myPeerInfo(): PeerInfo = networkNodes.myself

    override fun sigMaker(): SigMaker {
        return cryptoSystem.buildSigMaker(pubKey, privKey)
    }

    override fun verifier(): Verifier = cryptoSystem.makeVerifier()
}