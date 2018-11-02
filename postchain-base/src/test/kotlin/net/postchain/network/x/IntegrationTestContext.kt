package net.postchain.network.x

import com.nhaarman.mockitokotlin2.mock
import net.postchain.base.BasePeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.network.PacketConverter
import net.postchain.test.KeyPairHelper

class IntegrationTestContext(
        connectorFactory: XConnectorFactory,
        blockchainRid: ByteArray,
        peerInfos: Array<PeerInfo>,
        myIndex: Int,
        packetConverter: PacketConverter<Int>
) {
    val peerCommunicationConfig = BasePeerCommConfiguration(peerInfos, blockchainRid, myIndex, mock(), KeyPairHelper.privKey(myIndex))
    val connectionManager = DefaultXConnectionManager(connectorFactory, peerInfos[myIndex], packetConverter)
    val communicationManager = DefaultXCommunicationManager(connectionManager, peerCommunicationConfig, 1L, packetConverter)
}