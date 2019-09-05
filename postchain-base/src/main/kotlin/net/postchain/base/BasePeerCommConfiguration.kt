// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base

import net.postchain.common.toHex
import net.postchain.core.ByteArrayKey
import net.postchain.core.UserMistake
import net.postchain.network.x.XPeerID

class BasePeerCommConfiguration(override val networkNodes: NetworkNodes,
                                private val cryptoSystem: CryptoSystem,
                                private val privKey: ByteArray,
                                override val pubKey: ByteArray
) : PeerCommConfiguration {

    companion object {
        fun build(peerInfoArray: Array<PeerInfo>,
                  cryptoSystem: CryptoSystem,
                  privKey: ByteArray,
                  pubKey: ByteArray): BasePeerCommConfiguration {

            val peers: Collection<PeerInfo> = peerInfoArray.toSet()
            val nn = NetworkNodes.buildNetworkNodes(peers, ByteArrayKey(pubKey))
            return BasePeerCommConfiguration(nn, cryptoSystem, privKey, pubKey)
        }

        fun build(peerInfoMap: Map<XPeerID, PeerInfo>,
                  cryptoSystem: CryptoSystem,
                  privKey: ByteArray,
                  pubKey: ByteArray): BasePeerCommConfiguration {

            val nn = NetworkNodes.buildNetworkNodes(peerInfoMap.values, ByteArrayKey(pubKey))
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