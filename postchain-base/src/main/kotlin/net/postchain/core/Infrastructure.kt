// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

import net.postchain.base.HistoricBlockchain
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.debug.BlockchainProcessName
import net.postchain.debug.NodeDiagnosticContext

interface SynchronizationInfrastructure : Shutdownable {

    fun makeBlockchainProcess(
            processName: BlockchainProcessName,
            engine: BlockchainEngine,
            historicBlockchain: HistoricBlockchain? = null
    ): BlockchainProcess

}

interface BlockchainInfrastructure : SynchronizationInfrastructure {

    fun makeBlockchainConfiguration(rawConfigurationData: ByteArray,
                                    eContext: EContext,
                                    nodeId: Int,
                                    chainId: Long
    ): BlockchainConfiguration

    fun makeBlockchainEngine(
            processName: BlockchainProcessName,
            configuration: BlockchainConfiguration,
            restartHandler: RestartHandler
    ): BlockchainEngine

}

interface ApiInfrastructure : Shutdownable {
    fun connectProcess(process: BlockchainProcess)
    fun disconnectProcess(process: BlockchainProcess)
}

interface InfrastructureFactory {

    fun makeBlockchainConfigurationProvider(): BlockchainConfigurationProvider

    fun makeBlockchainInfrastructure(
            nodeConfigProvider: NodeConfigurationProvider,
            nodeDiagnosticContext: NodeDiagnosticContext
    ): BlockchainInfrastructure

    fun makeProcessManager(nodeConfigProvider: NodeConfigurationProvider,
                           blockchainInfrastructure: BlockchainInfrastructure,
                           blockchainConfigurationProvider: BlockchainConfigurationProvider,
                           nodeDiagnosticContext: NodeDiagnosticContext
    ): BlockchainProcessManager
}

enum class Infrastructures(val secondName: String) {
    BaseEbft("base/ebft"),
    BaseTest("base/test")
}

interface InfrastructureFactoryProvider {
    fun createInfrastructureFactory(nodeConfigProvider: NodeConfigurationProvider): InfrastructureFactory
}