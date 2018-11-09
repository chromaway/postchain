package net.postchain.network.x

import com.nhaarman.mockitokotlin2.mock
import net.postchain.base.BasePeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.network.PacketConverter
import net.postchain.test.KeyPairHelper

class IntegrationTestContext(
        connectorFactory: XConnectorFactory,
        blockchainRid: ByteArray,
        peerInfos: Array<PeerInfo>,
        myIndex: Int,
        packetConverter: PacketConverter<Int>
) {
    val cryptoSystem = SECP256K1CryptoSystem()
    val peerCommunicationConfig = BasePeerCommConfiguration(peerInfos, blockchainRid, myIndex, mock(), KeyPairHelper.privKey(myIndex))
    val connectionManager = DefaultXConnectionManager(connectorFactory, peerInfos[myIndex], packetConverter, cryptoSystem)
    val communicationManager = DefaultXCommunicationManager(connectionManager, peerCommunicationConfig, 1L, packetConverter)
}