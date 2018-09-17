package net.postchain.test.testinfra

import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.BlockchainConfigurationFactory
import net.postchain.core.BlockchainContext

class TestBlockchainConfigurationFactory : BlockchainConfigurationFactory {

    override fun makeBlockchainConfiguration(configData: Any, context: BlockchainContext?): BlockchainConfiguration {
        return TestBlockchainConfiguration(configData as BaseBlockchainConfigurationData)
    }
}