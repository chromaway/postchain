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
import net.postchain.network.PeerConnectionManager
import org.apache.commons.configuration2.Configuration

class EBFTSynchronizationInfrastructure(val config: Configuration) : SynchronizationInfrastructure {

    private val connManagers = mutableListOf<PeerConnectionManager<*>>()

    override fun shutdown() {
        connManagers.forEach { it.shutdown() }
    }

    override fun makeBlockchainProcess(engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess {
        val blockchainConfig = engine.getConfiguration() as BaseBlockchainConfiguration // TODO: [et]: Resolve type cast
        return EBFTBlockchainInstanceWorker(
                engine,
                blockchainConfig.configData.context.nodeID,
                buildCommunicationManager(blockchainConfig),
                restartHandler)
    }

    private fun buildCommunicationManager(blockchainConfig: BaseBlockchainConfiguration): CommManager<EbftMessage> {
        val communicationConfig = BasePeerCommConfiguration(
                PeerInfoCollectionFactory.createPeerInfoCollection(config),
                blockchainConfig.blockchainRID,
                blockchainConfig.configData.context.nodeID,
                SECP256K1CryptoSystem(),
                privKey())

        val connectionManager = EbftPeerManagerFactory.createConnectionManager(communicationConfig)
        connManagers.add(connectionManager)
        return CommManager(communicationConfig, connectionManager)
    }

    private fun privKey(): ByteArray =
            config.getString("messaging.privkey").hexStringToByteArray()
}