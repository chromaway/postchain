// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain

import net.postchain.base.BlockchainRid
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BaseInfrastructureFactoryProvider
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.Shutdownable
import net.postchain.debug.DefaultNodeDiagnosticContext
import net.postchain.debug.DiagnosticProperty

/**
 * Postchain node instantiates infrastructure and blockchain process manager.
 */
open class PostchainNode(val nodeConfigProvider: NodeConfigurationProvider) : Shutdownable {

    protected val blockchainInfrastructure: BlockchainInfrastructure
    val processManager: BlockchainProcessManager
    private val diagnosticContext = DefaultNodeDiagnosticContext()

    init {
        val infrastructureFactory = BaseInfrastructureFactoryProvider().createInfrastructureFactory(nodeConfigProvider)
        blockchainInfrastructure = infrastructureFactory.makeBlockchainInfrastructure(nodeConfigProvider, diagnosticContext)
        val blockchainConfigProvider = infrastructureFactory.makeBlockchainConfigurationProvider()
        processManager = infrastructureFactory.makeProcessManager(
                nodeConfigProvider, blockchainInfrastructure, blockchainConfigProvider, diagnosticContext)

        diagnosticContext.addProperty(DiagnosticProperty.VERSION, "3.0.1") // TODO: [POS-97]
        diagnosticContext.addProperty(DiagnosticProperty.PUB_KEY, nodeConfigProvider.getConfiguration().pubKey)
    }

    fun startBlockchain(chainId: Long): BlockchainRid? {
        return processManager.startBlockchain(chainId)
    }

    fun stopBlockchain(chainId: Long) {
        processManager.stopBlockchain(chainId)
    }

    override fun shutdown() {
        // FYI: Order is important
        processManager.shutdown()
        blockchainInfrastructure.shutdown()
        nodeConfigProvider.close()
    }
}
