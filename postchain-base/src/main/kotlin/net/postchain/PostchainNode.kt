// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.InfrastructureFactory
import net.postchain.core.Shutdownable
import org.apache.commons.configuration2.Configuration

/*
    Postchain node instantiates infrastructure and blockchain
    process manager.
 */

open class PostchainNode(nodeConfig: Configuration): Shutdownable {

    val processManager: BlockchainProcessManager
    protected val blockchainInfrastructure: BlockchainInfrastructure

    init {
        var infrastructureFactoryName = nodeConfig.getString("infrastructure",
                net.postchain.ebft.BaseEBFTInfrastructureFactory::class.qualifiedName)
        when (infrastructureFactoryName) {
            "base/ebft" ->
                infrastructureFactoryName = net.postchain.ebft.BaseEBFTInfrastructureFactory::class.qualifiedName
            "base/test" ->
                infrastructureFactoryName = net.postchain.base.BaseTestInfrastructureFactory::class.qualifiedName
        }

        val infrastructureFactoryClass = Class.forName(infrastructureFactoryName)
        val factory = (infrastructureFactoryClass.newInstance() as InfrastructureFactory)

        blockchainInfrastructure = factory.makeBlockchainInfrastructure(nodeConfig)

        processManager = factory.makeProcessManager(
                nodeConfig,
                blockchainInfrastructure
        )
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