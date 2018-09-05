package net.postchain.core

import org.apache.commons.configuration2.Configuration

interface SynchronizationInfrastructure {
    fun makeBlockchainProcess(engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess
}

interface BlockchainInfrastructure : SynchronizationInfrastructure {
    fun parseConfigurationString(rawData: String, format: String): ByteArray
    fun makeBlockchainConfiguration(rawConfigurationData: ByteArray, context: BlockchainContext): BlockchainConfiguration
    fun makeBlockchainEngine(configuration: BlockchainConfiguration): BlockchainEngine
}

interface InfrastructureFactory {
    fun makeBlockchainInfrastructure(config: Configuration): BlockchainInfrastructure
}
