// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.BaseTestInfrastructureFactory
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.InfrastructureFactory
import net.postchain.core.Shutdownable
import net.postchain.ebft.BaseEBFTInfrastructureFactory
import org.apache.commons.configuration2.Configuration

/**
 * Postchain node instantiates infrastructure and blockchain
 * process manager.
 */
open class PostchainNode(nodeConfig: Configuration) : Shutdownable {

    val processManager: BlockchainProcessManager
    protected val blockchainInfrastructure: BlockchainInfrastructure

    init {
        val factoryClass = when (nodeConfig.getString("infrastructure")) {
            "base/ebft" -> BaseEBFTInfrastructureFactory::class.java
            "base/test" -> BaseTestInfrastructureFactory::class.java
            else -> BaseEBFTInfrastructureFactory::class.java
        }

        val factory: InfrastructureFactory = factoryClass.newInstance()
        blockchainInfrastructure = factory.makeBlockchainInfrastructure(nodeConfig)
        processManager = factory.makeProcessManager(nodeConfig, blockchainInfrastructure)
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
}