package net.postchain.ebft

import net.postchain.base.BasePeerCommConfiguration
import net.postchain.base.PeerCommConfiguration
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.hexStringToByteArray
import net.postchain.config.node.NodeConfig
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.ebft.message.Message
import net.postchain.ebft.worker.ReadOnlyWorker
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
                buildPeerCommConfiguration(nodeConfig),
                EbftPacketEncoderFactory(),
                EbftPacketDecoderFactory(),
                SECP256K1CryptoSystem()
        )
    }

    override fun shutdown() {
        connectionManager.shutdown()
    }

    override fun makeBlockchainProcess(processName: String, engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess {
        val blockchainConfig = engine.getConfiguration() as BaseBlockchainConfiguration // TODO: [et]: Resolve type cast
        validateConfigurations(nodeConfig, blockchainConfig)
        return if (blockchainConfig.configData.context.nodeID != NODE_ID_READ_ONLY) {
            ValidatorWorker(
                    processName,
                    blockchainConfig.signers,
                    engine,
                    blockchainConfig.configData.context.nodeID,
                    buildXCommunicationManager(processName, blockchainConfig),
                    restartHandler)
        } else {
            ReadOnlyWorker(
                    processName,
                    blockchainConfig.signers,
                    engine,
                    buildXCommunicationManager(processName, blockchainConfig),
                    restartHandler)
        }
    }

    private fun validateConfigurations(nodeConfig: NodeConfig, blockchainConfig: BaseBlockchainConfiguration) {
        val nodePeers = nodeConfig.peerInfos.map { it.pubKey.byteArrayKeyOf() }
        val chainPeers = blockchainConfig.signers.map { it.byteArrayKeyOf() }

        require(chainPeers.all { nodePeers.contains(it) }) {
            "Invalid blockchain config: unreachable signers have been detected"
        }
    }

    private fun buildXCommunicationManager(processName: String, blockchainConfig: BaseBlockchainConfiguration): CommunicationManager<Message> {
        val communicationConfig = BasePeerCommConfiguration(
                nodeConfig.peerInfos,
                SECP256K1CryptoSystem(),
                nodeConfig.privKeyByteArray,
                nodeConfig.pubKey.hexStringToByteArray())

        val packetEncoder = EbftPacketEncoder(communicationConfig, blockchainConfig.blockchainRID)
        val packetDecoder = EbftPacketDecoder(communicationConfig)

        return DefaultXCommunicationManager(
                connectionManager,
                communicationConfig,
                blockchainConfig.chainID,
                blockchainConfig.blockchainRID,
                packetEncoder,
                packetDecoder,
                processName
        ).apply { init() }
    }

    private fun buildPeerCommConfiguration(nodeConfig: NodeConfig): PeerCommConfiguration {
        return BasePeerCommConfiguration(
                nodeConfig.peerInfos,
                SECP256K1CryptoSystem(),
                nodeConfig.privKeyByteArray,
                nodeConfig.pubKey.hexStringToByteArray())
    }
}