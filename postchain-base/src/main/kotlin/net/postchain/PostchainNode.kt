// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain

import mu.KLogging
import net.postchain.base.BlockchainRid
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BaseInfrastructureFactoryProvider
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.Shutdownable
import net.postchain.debug.DefaultNodeDiagnosticContext
import net.postchain.debug.DiagnosticProperty
import net.postchain.devtools.PeerNameHelper.peerName

/**
 * Postchain node instantiates infrastructure and blockchain process manager.
 */
open class PostchainNode(val nodeConfigProvider: NodeConfigurationProvider) : Shutdownable {

    protected val blockchainInfrastructure: BlockchainInfrastructure
    val processManager: BlockchainProcessManager
    private val diagnosticContext = DefaultNodeDiagnosticContext()

    companion object : KLogging()

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
        logger.debug("${name()}: Stopping ProcessManager")
        processManager.shutdown()
        logger.debug("${name()}: Stopping BlockchainInfrastructure")
        blockchainInfrastructure.shutdown()
        logger.debug("${name()}: Closing NodeConfigurationProvider")
        nodeConfigProvider.close()
        logger.debug("${name()}: Stopped PostchainNode")
    }

    private fun name(): String {
        return peerName(nodeConfigProvider.getConfiguration().pubKey)
    }
}
