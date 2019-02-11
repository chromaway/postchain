package net.postchain.ebft

import net.postchain.base.*
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.hexStringToByteArray
import net.postchain.core.BlockchainEngine
import net.postchain.core.BlockchainProcess
import net.postchain.core.RestartHandler
import net.postchain.core.SynchronizationInfrastructure
import net.postchain.ebft.message.EbftMessage
import net.postchain.network.CommunicationManager
import net.postchain.network.IdentPacketConverter
import net.postchain.network.netty.NettyConnectorFactory
import net.postchain.network.x.DefaultXCommunicationManager
import net.postchain.network.x.DefaultXConnectionManager
import net.postchain.network.x.XConnectionManager
import org.apache.commons.configuration2.Configuration

class EBFTSynchronizationInfrastructure(val config: Configuration) : SynchronizationInfrastructure {

    private val connectionManagers = mutableListOf<XConnectionManager>()

    override fun shutdown() {
        connectionManagers.forEach { it.shutdown() }
    }

    override fun makeBlockchainProcess(engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess {
        val blockchainConfig = engine.getConfiguration() as BaseBlockchainConfiguration // TODO: [et]: Resolve type cast

        val communicationConfig = buildBasePeerCommConfig(blockchainConfig)
        val defaultXConnectionManager = buildXConnectionManager(
                communicationConfig.peerInfo[communicationConfig.myIndex],
                EbftPacketConverter(communicationConfig)
        )
        val defaultXCommunicationManager = buildXCommunicationManager(
                defaultXConnectionManager,
                communicationConfig,
                blockchainConfig.chainID
        )
        return EBFTBlockchainInstanceWorker(
                engine,
                nodeIndex = blockchainConfig.configData.context.nodeID,
                communicationManager = defaultXCommunicationManager,
                restartHandler = restartHandler)
    }

    private fun buildBasePeerCommConfig(blockchainConfig: BaseBlockchainConfiguration): BasePeerCommConfiguration {
        return BasePeerCommConfiguration(
                PeerInfoCollectionFactory.createPeerInfoCollection(config),
                blockchainConfig.blockchainRID,
                blockchainConfig.configData.context.nodeID,
                SECP256K1CryptoSystem(),
                privKey())
    }

    private fun buildXConnectionManager(peerInfo: PeerInfo, identPacketConverter: IdentPacketConverter): XConnectionManager {
        return DefaultXConnectionManager(
                NettyConnectorFactory(),
                peerInfo,
                identPacketConverter,
                SECP256K1CryptoSystem()
        ).also { connectionManagers.add(it) }
    }

    private fun buildXCommunicationManager(connectionManager: XConnectionManager, communicationConfig: PeerCommConfiguration, chainID: Long)
            : CommunicationManager<EbftMessage> {
        return DefaultXCommunicationManager(
                connectionManager,
                communicationConfig,
                chainID,
                EbftPacketConverter(communicationConfig))
    }

    private fun privKey(): ByteArray =
            config.getString("messaging.privkey").hexStringToByteArray()
}