package net.postchain.managed

import net.postchain.base.BaseBlockchainProcessManager
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BlockchainInfrastructure

class ManagedBlockchainProcessManager(
        blockchainInfrastructure: BlockchainInfrastructure,
        nodeConfigProvider: NodeConfigurationProvider,
        blockchainConfigProvider: BlockchainConfigurationProvider

) : BaseBlockchainProcessManager(
        blockchainInfrastructure,
        nodeConfigProvider,
        blockchainConfigProvider
)
