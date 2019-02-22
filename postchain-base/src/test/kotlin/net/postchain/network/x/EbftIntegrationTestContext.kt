package net.postchain.network.x

import net.postchain.base.PeerCommConfiguration
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.ebft.EbftPacketDecoder
import net.postchain.ebft.EbftPacketDecoderFactory
import net.postchain.ebft.EbftPacketEncoder
import net.postchain.ebft.EbftPacketEncoderFactory
import net.postchain.ebft.message.EbftMessage
import net.postchain.network.netty2.NettyConnectorFactory
import java.io.Closeable

class EbftIntegrationTestContext(
        config: PeerCommConfiguration,
        blockchainRid: ByteArray
) : Closeable {

    val chainId = 1L
    private val connectorFactory = NettyConnectorFactory<EbftMessage>()

    val connectionManager = DefaultXConnectionManager(
            connectorFactory,
            config,
            EbftPacketEncoderFactory(),
            EbftPacketDecoderFactory(),
            SECP256K1CryptoSystem())

    val communicationManager = DefaultXCommunicationManager(
            connectionManager,
            config,
            chainId,
            EbftPacketEncoder(config, blockchainRid),
            EbftPacketDecoder(config))

    fun shutdown() {
        communicationManager.shutdown()
        connectionManager.shutdown()
    }

    override fun close() {
        shutdown()
    }
}