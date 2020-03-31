package net.postchain.integrationtest.statemachine

import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.common.toHex
import net.postchain.core.BlockchainConfiguration
import net.postchain.devtools.testinfra.TestBlockchainConfiguration
import net.postchain.devtools.testinfra.TestBlockchainConfigurationFactory

class FailableTestBlockchainConfigurationFactory : TestBlockchainConfigurationFactory() {

    override fun makeBlockchainConfiguration(configData: Any): BlockchainConfiguration {
        val owner = (configData as BaseBlockchainConfigurationData)
                .context.nodeRID!!.toHex().toUpperCase()

        /*return TestBlockchainConfiguration(
                configData,
                createGtxModule(configData.context.blockchainRID, configData.data)
        )*/

        return if (owner == Nodes.pubKey0) {
            TestBlockchainConfiguration(
                    configData,
                    createGtxModule(configData.context.blockchainRID, configData.data)
            )
        } else {
            FailableTestBlockchainConfiguration(
                    configData,
                    createGtxModule(configData.context.blockchainRID, configData.data)
            )
        }

    }
}