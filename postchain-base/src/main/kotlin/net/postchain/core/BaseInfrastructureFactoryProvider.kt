// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

import net.postchain.base.BaseTestInfrastructureFactory
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.ebft.BaseEBFTInfrastructureFactory

/**
 * Provides infrastructure factory object based on `infrastructure` field of [net.postchain.config.node.NodeConfig]
 */
class BaseInfrastructureFactoryProvider : InfrastructureFactoryProvider {

    override fun createInfrastructureFactory(nodeConfigProvider: NodeConfigurationProvider): InfrastructureFactory {
        val infrastructure = nodeConfigProvider.getConfiguration().infrastructure
        val factoryClass = when (infrastructure) {
            Infrastructures.BaseEbft.secondName.toLowerCase() -> BaseEBFTInfrastructureFactory::class.java
            Infrastructures.BaseTest.secondName.toLowerCase() -> BaseTestInfrastructureFactory::class.java
            else -> Class.forName(infrastructure)
        }
        return factoryClass.newInstance() as InfrastructureFactory
    }
}