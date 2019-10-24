package net.postchain.managed

import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.ebft.BaseEBFTInfrastructureFactory

class ManagedEBFTInfrastructureFactory : BaseEBFTInfrastructureFactory() {

    override fun makeBlockchainConfigurationProvider(): BlockchainConfigurationProvider {
        return ManagedBlockchainConfigurationProvider()
    }
}