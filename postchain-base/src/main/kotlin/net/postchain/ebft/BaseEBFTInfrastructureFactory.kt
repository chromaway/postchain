package net.postchain.ebft

import net.postchain.base.BaseApiInfrastructure
import net.postchain.base.BaseBlockchainInfrastructure
import net.postchain.base.BaseBlockchainProcessManager
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.InfrastructureFactory
import org.apache.commons.configuration2.Configuration

class BaseEBFTInfrastructureFactory: InfrastructureFactory {
    override fun makeBlockchainInfrastructure(config: Configuration): BlockchainInfrastructure {
        return BaseBlockchainInfrastructure(
                config,
                EBFTSynchronizationInfrastructure(config),
                BaseApiInfrastructure(config))
    }

    override fun makeProcessManager(
            config: Configuration,
            blockchainInfrastructure: BlockchainInfrastructure
            ): BlockchainProcessManager {
        return BaseBlockchainProcessManager(
                blockchainInfrastructure,
                config)
    }
}