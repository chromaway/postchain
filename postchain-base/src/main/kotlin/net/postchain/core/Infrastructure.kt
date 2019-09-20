package net.postchain.core

import net.postchain.base.Storage
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider

interface SynchronizationInfrastructure : Shutdownable {
    fun makeBlockchainProcess(processName: String, engine: BlockchainEngine, restartHandler: RestartHandler): BlockchainProcess
}

interface BlockchainInfrastructure : SynchronizationInfrastructure {
    @Deprecated("TODO: Remove it after Managed Mode is done")
    fun parseConfigurationString(rawData: String, format: String): ByteArray

    fun makeBlockchainConfiguration(rawConfigurationData: ByteArray, context: BlockchainContext): BlockchainConfiguration
    fun makeBlockchainEngine(configuration: BlockchainConfiguration): BlockchainEngine

    fun makeStorage(): Storage
}

interface ApiInfrastructure : Shutdownable {
    fun connectProcess(process: BlockchainProcess)
    fun disconnectProcess(process: BlockchainProcess)
}

interface InfrastructureFactory {
    fun makeBlockchainConfigurationProvider(): BlockchainConfigurationProvider
    fun makeBlockchainInfrastructure(nodeConfigProvider: NodeConfigurationProvider): BlockchainInfrastructure
    fun makeProcessManager(nodeConfigProvider: NodeConfigurationProvider,
                           blockchainInfrastructure: BlockchainInfrastructure,
                           blockchainConfigurationProvider: BlockchainConfigurationProvider
    ): BlockchainProcessManager
}

enum class Infrastructures(val secondName: String) {
    BaseEbft("base/ebft"),
    BaseTest("base/test")
}