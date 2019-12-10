package net.postchain.network.x

import net.postchain.base.*

/**
 * A stupid stub only used in testing (to avoid the need for mocking)
 */
open class PeerCommConfigurationDummy: PeerCommConfiguration {
    override val networkNodes: NetworkNodes = NetworkNodes.buildNetworkNodesDummy()
    override val pubKey: ByteArray = ByteArray(1)
    override fun resolvePeer(peerID: PeerID): PeerInfo? = null
    override fun myPeerInfo(): PeerInfo = throw NotImplementedError("Not impl")
    override fun sigMaker(): SigMaker = throw NotImplementedError("Not impl")
    override fun verifier(): Verifier = throw NotImplementedError("Not impl")
}

