package net.postchain.core

import net.postchain.api.rest.controller.Model
import net.postchain.base.PeerInfo
import net.postchain.ebft.CommManager
import net.postchain.ebft.message.EbftMessage
import org.apache.commons.configuration2.Configuration

interface SynchronizationInfrastructure {
    fun makeBlockchainProcess(
            engine: BlockchainEngine,
            communicationManager: CommManager<EbftMessage>,
            restartHandler: RestartHandler
    ): BlockchainProcess
}

interface BlockchainInfrastructure : SynchronizationInfrastructure {
    fun parseConfigurationString(rawData: String, format: String): ByteArray
    fun makeBlockchainConfiguration(rawConfigurationData: ByteArray, context: BlockchainContext): BlockchainConfiguration
    fun makeBlockchainEngine(configuration: BlockchainConfiguration, wipeDatabase: Boolean): BlockchainEngine
}

interface InfrastructureFactory {
    fun makeBlockchainInfrastructure(config: Configuration): BlockchainInfrastructure
}

interface ApiInfrastructure {
    fun connectProcess(process: BlockchainProcess, communicationManager: CommManager<EbftMessage>)
    fun disconnectProcess(process: BlockchainProcess)
    fun shutdown()
    fun getApiModel(process: BlockchainProcess): Model?
}

interface NetworkInfrastructure {
    val peers: Array<PeerInfo>
    fun buildCommunicationManager(configuration: BlockchainConfiguration): CommManager<EbftMessage>
}
