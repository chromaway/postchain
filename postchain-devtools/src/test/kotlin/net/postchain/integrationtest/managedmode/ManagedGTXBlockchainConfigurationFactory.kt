// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.managedmode

import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.core.BlockchainConfiguration
import net.postchain.gtx.GTXBlockchainConfiguration
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.gtx.GTXModule

class ManagedGTXBlockchainConfigurationFactory : GTXBlockchainConfigurationFactory() {

    companion object {
        val moduleRegistry = mutableMapOf<String, GTXModule>()
    }

    override fun makeBlockchainConfiguration(configurationData: Any): BlockchainConfiguration {
        val baseConfigData = configurationData as BaseBlockchainConfigurationData
        val module = createGtxModule(baseConfigData.context.blockchainRID, baseConfigData.data)
        val configuration = GTXBlockchainConfiguration(baseConfigData, module)
        moduleRegistry[module.javaClass.simpleName] = module
        return configuration
    }
}