package net.postchain.ebft

import net.postchain.base.BasePeerCommConfiguration
import net.postchain.base.PeerInfoCollectionFactory
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.hexStringToByteArray
import net.postchain.core.BlockchainEngine
import net.postchain.core.BlockchainProcess
import net.postchain.core.RestartHandler
import net.postchain.core.SynchronizationInfrastructure
import net.postchain.ebft.message.EbftMessage
import net.postchain.network.CommManager
import net.postchain.network.CommunicationManager
import net.postchain.network.PeerConnectionManager
import net.postchain.network.netty.NettyConnectorFactory
import net.postchain.network.x.DefaultXCommunicationManager
import net.postchain.network.x.DefaultXConnectionManager
import net.postchain.network.x.XConnectionManager
import org.apache.commons.configuration2.Configuration

class EBFTSynchronizationInfrastructure(val config: Configuration) : SynchronizationInfrastructure {

    private val connectionManagers = mutableListOf<XConnectionManager>()
    @Deprecated("See XCommunicationManager")
    private val connectionManagersDeprecated = mutableListOf<PeerConnectionManager<*>>()

    override fun shutdown() {
        connectionManagers.forEach { it.shutdown() }
        connectionManagersDeprecated.forEach { it.shutdown() }
    }

    override fun makeBlockchainProcess(engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess {
        val blockchainConfig = engine.getConfiguration() as BaseBlockchainConfiguration // TODO: [et]: Resolve type cast
        return EBFTBlockchainInstanceWorker(
                engine,
                blockchainConfig.configData.context.nodeID,
                buildCommunicationManager(blockchainConfig),
//                buildXCommunicationManager(blockchainConfig),
                restartHandler)
    }

    private fun buildXCommunicationManager(blockchainConfig: BaseBlockchainConfiguration): CommunicationManager<EbftMessage> {
        val communicationConfig = BasePeerCommConfiguration(
                PeerInfoCollectionFactory.createPeerInfoCollection(config),
                blockchainConfig.blockchainRID,
                blockchainConfig.configData.context.nodeID,
                SECP256K1CryptoSystem(),
                privKey())

        val packetConverter = EbftPacketConverter(communicationConfig)

        val connectionManager = DefaultXConnectionManager(
                NettyConnectorFactory(),
                communicationConfig.peerInfo[communicationConfig.myIndex],
                packetConverter,
                SECP256K1CryptoSystem()
        ).also { connectionManagers.add(it) }

        return DefaultXCommunicationManager(
                connectionManager,
                communicationConfig,
                blockchainConfig.chainID,
                packetConverter)
    }

    @Deprecated("See XCommunicationManager")
    private fun buildCommunicationManager(blockchainConfig: BaseBlockchainConfiguration): CommManager<EbftMessage> {
        val communicationConfig = BasePeerCommConfiguration(
                PeerInfoCollectionFactory.createPeerInfoCollection(config),
                blockchainConfig.blockchainRID,
                blockchainConfig.configData.context.nodeID,
                SECP256K1CryptoSystem(),
                privKey())

        val connectionManager = EbftPeerManagerFactory.createConnectionManager(communicationConfig)
        connectionManagersDeprecated.add(connectionManager)
        return CommManager(communicationConfig, connectionManager)
    }

    private fun privKey(): ByteArray =
            config.getString("messaging.privkey").hexStringToByteArray()
}