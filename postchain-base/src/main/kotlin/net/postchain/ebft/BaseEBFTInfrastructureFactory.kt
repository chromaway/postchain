package net.postchain.ebft

import net.postchain.base.BaseApiInfrastructure
import net.postchain.base.BaseBlockchainInfrastructure
import net.postchain.base.BaseBlockchainProcessManager
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.blockchain.ManualBlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.InfrastructureFactory
import net.postchain.core.RestartHandler

open class BaseEBFTInfrastructureFactory : InfrastructureFactory {

    override fun makeBlockchainConfigurationProvider(): BlockchainConfigurationProvider {
        return ManualBlockchainConfigurationProvider()
    }

    override fun makeBlockchainInfrastructure(nodeConfigProvider: NodeConfigurationProvider): BlockchainInfrastructure {
        return BaseBlockchainInfrastructure(
                nodeConfigProvider,
                EBFTSynchronizationInfrastructure(nodeConfigProvider),
                BaseApiInfrastructure(nodeConfigProvider))
    }

    override fun makeProcessManager(
            nodeConfigProvider: NodeConfigurationProvider,
            blockchainInfrastructure: BlockchainInfrastructure,
            blockchainConfigurationProvider: BlockchainConfigurationProvider,
            restartHandlerFactory: (chainId: Long) -> RestartHandler
    ): BlockchainProcessManager {

        return BaseBlockchainProcessManager(
                blockchainInfrastructure,
                nodeConfigProvider,
                blockchainConfigurationProvider,
                restartHandlerFactory)
    }
}