package net.postchain.core

interface SynchronizationInfrastructure: Shutdownable {
    fun makeBlockchainProcess(engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess
}

interface BlockchainInfrastructure : SynchronizationInfrastructure {
    fun parseConfigurationString(rawData: String, format: String): ByteArray
    fun makeBlockchainConfiguration(rawConfigurationData: ByteArray, context: BlockchainContext): BlockchainConfiguration
    fun makeBlockchainEngine(configuration: BlockchainConfiguration): BlockchainEngine
}

interface ApiInfrastructure: Shutdownable {
    fun connectProcess(process: BlockchainProcess)
    fun disconnectProcess(process: BlockchainProcess)
}
