// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

import net.postchain.base.HistoricBlockchainContext
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.debug.BlockTrace
import net.postchain.debug.BlockchainProcessName
import net.postchain.debug.NodeDiagnosticContext

/**
 * Responsible blockchain process lifecycle, i.e. creating, exiting and restarting blockchain processes.
 */
interface SynchronizationInfrastructure : Shutdownable {

    /**
     * This is how a blockchain process get created.
     */
    fun makeBlockchainProcess(
        processName: BlockchainProcessName,
        engine: BlockchainEngine,
        historicBlockchainContext: HistoricBlockchainContext? = null
    ): BlockchainProcess

    /**
     * Call this hook upon blockchain process restart.
     * Note: responsible for keeping track of the two BC process sync modes (normal sync and fastsync)
     */
    fun restartBlockchainProcess(process: BlockchainProcess)

    /**
     * Call this hook before blockchain process is killed.
     * Note: responsible for keeping track of the two BC process sync modes (normal sync and fastsync)
     */
    fun exitBlockchainProcess(process: BlockchainProcess)
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
            restartHandler: (BlockTrace?) -> Boolean
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