package net.postchain.ebft

import net.postchain.base.*
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.BlockchainEngine
import net.postchain.core.BlockchainProcess
import net.postchain.core.RestartHandler
import net.postchain.core.SynchronizationInfrastructure
import net.postchain.core.*
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.worker.ReadOnlyWorker
import net.postchain.ebft.worker.ValidatorWorker
import net.postchain.network.CommunicationManager
import net.postchain.network.netty2.NettyConnectorFactory
import net.postchain.network.x.DefaultXCommunicationManager
import net.postchain.network.x.DefaultXConnectionManager
import net.postchain.network.x.XConnectionManager
import org.apache.commons.configuration2.Configuration

class EBFTSynchronizationInfrastructure(val config: Configuration) : SynchronizationInfrastructure {

    /*private */val connectionManager: XConnectionManager
    private val peers = PeerInfoCollectionFactory.createPeerInfoCollection(config)

    init {
        connectionManager = DefaultXConnectionManager(
                NettyConnectorFactory(),
                buildPeerCommConfiguration(peers),
                EbftPacketEncoderFactory(),
                EbftPacketDecoderFactory(),
                SECP256K1CryptoSystem()
        )
    }

    override fun shutdown() {
        connectionManager.shutdown()
    }

    override fun makeBlockchainProcess(engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess {
        val blockchainConfig = engine.getConfiguration() as BaseBlockchainConfiguration // TODO: [et]: Resolve type cast
        return if(blockchainConfig.configData.context.nodeID != NODE_ID_READ_ONLY) {
            ValidatorWorker(
                    blockchainConfig.signers,
                    engine,
                    blockchainConfig.configData.context.nodeID,
                    buildXCommunicationManager(blockchainConfig),
                    restartHandler)
        } else {
            ReadOnlyWorker(
                    blockchainConfig.signers,
                    engine,
                    buildXCommunicationManager(blockchainConfig),
                    restartHandler
            )
        }
    }

    private fun buildXCommunicationManager(blockchainConfig: BaseBlockchainConfiguration): CommunicationManager<EbftMessage> {
        val communicationConfig = BasePeerCommConfiguration(
                PeerInfoCollectionFactory.createPeerInfoCollection(config),
                SECP256K1CryptoSystem(),
                privKey(),
                pubKey())

        val packetEncoder = EbftPacketEncoder(communicationConfig, blockchainConfig.blockchainRID)
        val packetDecoder = EbftPacketDecoder(communicationConfig)

        return DefaultXCommunicationManager(
                connectionManager,
                communicationConfig,
                blockchainConfig.chainID,
                blockchainConfig.blockchainRID,
                packetEncoder,
                packetDecoder
        ).apply { init() }
    }

    private fun privKey(): ByteArray =
            config.getString("messaging.privkey").hexStringToByteArray()

    private fun pubKey(): ByteArray =
            config.getString("messaging.pubkey").hexStringToByteArray()

    private fun buildPeerCommConfiguration(peers: Array<PeerInfo>): PeerCommConfiguration {
        return BasePeerCommConfiguration(
                peers,
                SECP256K1CryptoSystem(),
                privKey(),
                pubKey())
    }
}