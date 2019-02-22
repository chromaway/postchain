package net.postchain.network.x

import com.nhaarman.mockitokotlin2.mock
import net.postchain.base.BasePeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.devtools.KeyPairHelper

class IntegrationTestContext(
        connectorFactory: XConnectorFactory<Int>,
        blockchainRid: ByteArray,
        peerInfos: Array<PeerInfo>,
        myIndex: Int
) {
    val peerCommunicationConfig = BasePeerCommConfiguration(
            peerInfos, blockchainRid, myIndex, mock(), KeyPairHelper.privKey(myIndex))

    val connectionManager = DefaultXConnectionManager(
            connectorFactory, mock(), mock(), mock(), SECP256K1CryptoSystem())

    val communicationManager = DefaultXCommunicationManager<Int>(
            connectionManager, peerCommunicationConfig, 1L, mock(), mock())

    fun shutdown() {
        communicationManager.shutdown()
        connectionManager.shutdown()
    }
}