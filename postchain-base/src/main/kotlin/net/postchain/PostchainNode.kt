// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.BaseTestInfrastructureFactory
import net.postchain.config.blockchain.BlockchainConfigurationProviderFactory
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.InfrastructureFactory
import net.postchain.core.Shutdownable
import net.postchain.ebft.BaseEBFTInfrastructureFactory

/**
 * Postchain node instantiates infrastructure and blockchain
 * process manager.
 */
open class PostchainNode(nodeConfigProvider: NodeConfigurationProvider) : Shutdownable {

    val processManager: BlockchainProcessManager
    protected val blockchainInfrastructure: BlockchainInfrastructure

    init {
        val blockchainConfig = BlockchainConfigurationProviderFactory.create(nodeConfigProvider)
        val infrastructureFactory = buildInfrastructureFactory(nodeConfigProvider)

        blockchainInfrastructure = infrastructureFactory.makeBlockchainInfrastructure(nodeConfigProvider)
        processManager = infrastructureFactory.makeProcessManager(nodeConfigProvider, blockchainConfig, blockchainInfrastructure)
    }

    fun startBlockchain(chainID: Long) {
        processManager.startBlockchain(chainID)
    }

    fun stopBlockchain(chainID: Long) {
        processManager.stopBlockchain(chainID)
    }

    override fun shutdown() {
        processManager.shutdown()
    }

    private fun buildInfrastructureFactory(nodeConfigProvider: NodeConfigurationProvider): InfrastructureFactory {
        val factoryClass = when (nodeConfigProvider.getConfiguration().infrastructure) {
            "base/ebft" -> BaseEBFTInfrastructureFactory::class.java
            "base/test" -> BaseTestInfrastructureFactory::class.java
            else -> BaseEBFTInfrastructureFactory::class.java
        }

        return factoryClass.newInstance()
    }
}