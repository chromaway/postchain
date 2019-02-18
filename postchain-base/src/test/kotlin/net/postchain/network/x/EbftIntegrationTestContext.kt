package net.postchain.network.x

import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.ebft.EbftPacketConverter
import net.postchain.network.netty2.NettyConnectorFactory
import java.io.Closeable

class EbftIntegrationTestContext(
        peerInfo: PeerInfo,
        config: PeerCommConfiguration
) : Closeable {

    val chainId = 1L
    private val packetConverter = EbftPacketConverter(config)
    private val connectorFactory = NettyConnectorFactory<EbftPacketConverter>()

    val connectionManager = DefaultXConnectionManager(
            connectorFactory, peerInfo, packetConverter, SECP256K1CryptoSystem())

    val communicationManager = DefaultXCommunicationManager(
            connectionManager, config, chainId, packetConverter)

    fun shutdown() {
        communicationManager.shutdown()
        connectionManager.shutdown()
    }

    override fun close() {
        shutdown()
    }
}