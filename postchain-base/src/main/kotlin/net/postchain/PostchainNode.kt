// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BaseInfrastructureFactoryProvider
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.Shutdownable

/**
 * Postchain node instantiates infrastructure and blockchain process manager.
 */
open class PostchainNode(val nodeConfigProvider: NodeConfigurationProvider) : Shutdownable {

    protected val blockchainInfrastructure: BlockchainInfrastructure
    val processManager: BlockchainProcessManager

    init {
        val infrastructureFactory = BaseInfrastructureFactoryProvider().createInfrastructureFactory(nodeConfigProvider)
        blockchainInfrastructure = infrastructureFactory.makeBlockchainInfrastructure(nodeConfigProvider)
        val blockchainConfigProvider = infrastructureFactory.makeBlockchainConfigurationProvider()
        processManager = infrastructureFactory.makeProcessManager(
                nodeConfigProvider, blockchainInfrastructure, blockchainConfigProvider)
    }

    fun startBlockchain(chainId: Long): ByteArray? {
        return processManager.startBlockchain(chainId)
    }

    fun stopBlockchain(chainId: Long) {
        processManager.stopBlockchain(chainId)
    }

    override fun shutdown() {
        // FYI: Order is important
        processManager.shutdown()
        blockchainInfrastructure.shutdown()
    }
}
