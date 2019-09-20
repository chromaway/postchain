package net.postchain.managed

import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.ebft.BaseEBFTInfrastructureFactory

class ManagedEBFTInfrastructureFactory : BaseEBFTInfrastructureFactory() {

    override fun makeBlockchainConfigurationProvider(): BlockchainConfigurationProvider {
        return ManagedBlockchainConfigurationProvider()
    }

    override fun makeProcessManager(
            nodeConfigProvider: NodeConfigurationProvider,
            blockchainInfrastructure: BlockchainInfrastructure,
            blockchainConfigurationProvider: BlockchainConfigurationProvider
    ): BlockchainProcessManager {

        return ManagedBlockchainProcessManager(
                blockchainInfrastructure,
                nodeConfigProvider,
                blockchainConfigurationProvider)
    }
}