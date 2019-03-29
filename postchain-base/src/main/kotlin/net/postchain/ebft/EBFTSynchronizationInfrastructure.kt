package net.postchain.ebft

import net.postchain.base.*
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BlockchainEngine
import net.postchain.core.BlockchainProcess
import net.postchain.core.RestartHandler
import net.postchain.core.SynchronizationInfrastructure
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.worker.ValidatorWorker
import net.postchain.network.CommunicationManager
import net.postchain.network.netty2.NettyConnectorFactory
import net.postchain.network.x.DefaultXCommunicationManager
import net.postchain.network.x.DefaultXConnectionManager
import net.postchain.network.x.XConnectionManager

class EBFTSynchronizationInfrastructure(nodeConfigProvider: NodeConfigurationProvider) : SynchronizationInfrastructure {

    private val nodeConfig = nodeConfigProvider.getConfiguration()
    val connectionManager: XConnectionManager

    init {
        connectionManager = DefaultXConnectionManager(
                NettyConnectorFactory(),
                buildPeerCommConfiguration(nodeConfig.peerInfos),
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
        return ValidatorWorker(
                blockchainConfig.signers,
                engine,
                blockchainConfig.configData.context.nodeID,
                buildXCommunicationManager(blockchainConfig),
                restartHandler)
    }

    private fun buildXCommunicationManager(blockchainConfig: BaseBlockchainConfiguration): CommunicationManager<EbftMessage> {
        val communicationConfig = BasePeerCommConfiguration(
                nodeConfig.peerInfos,
                blockchainConfig.configData.context.nodeID,
                SECP256K1CryptoSystem(),
                nodeConfig.privKeyByteArray)

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

    private fun buildPeerCommConfiguration(peers: Array<PeerInfo>): PeerCommConfiguration {
        return BasePeerCommConfiguration(
                peers,
                DefaultPeerResolver.resolvePeerIndex(nodeConfig.pubKeyByteArray, peers),
                SECP256K1CryptoSystem(),
                nodeConfig.privKeyByteArray)
    }
}