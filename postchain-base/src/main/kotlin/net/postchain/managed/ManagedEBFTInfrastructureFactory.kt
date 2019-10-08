package net.postchain.managed

import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.RestartHandler
import net.postchain.ebft.BaseEBFTInfrastructureFactory

class ManagedEBFTInfrastructureFactory : BaseEBFTInfrastructureFactory() {

    override fun makeBlockchainConfigurationProvider(): BlockchainConfigurationProvider {
        return ManagedBlockchainConfigurationProvider()
    }

    override fun makeProcessManager(
            nodeConfigProvider: NodeConfigurationProvider,
            blockchainInfrastructure: BlockchainInfrastructure,
            blockchainConfigurationProvider: BlockchainConfigurationProvider,
            restartHandlerFactory: (chainId: Long) -> RestartHandler
    ): BlockchainProcessManager {

        return ManagedBlockchainProcessManager(
                blockchainInfrastructure,
                nodeConfigProvider,
                blockchainConfigurationProvider,
                restartHandlerFactory)
    }
}