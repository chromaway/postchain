package net.postchain.devtools.testinfra

import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.BlockchainConfigurationFactory

class TestBlockchainConfigurationFactory : BlockchainConfigurationFactory {

    override fun makeBlockchainConfiguration(configData: Any): BlockchainConfiguration {
        return TestBlockchainConfiguration(configData as BaseBlockchainConfigurationData)
    }
}