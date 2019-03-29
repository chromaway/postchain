package net.postchain.config.blockchain

import net.postchain.config.node.NodeConfigurationProvider

object BlockchainConfigurationProviderFactory {

    fun create(nodeConfigProvider: NodeConfigurationProvider): BlockchainConfigurationProvider {
        val blockchainConfigProvider = nodeConfigProvider.getConfiguration().blockchainConfigProvider
        return when (blockchainConfigProvider) {
            "manual" -> ManualBlockchainConfigurationProvider(nodeConfigProvider)
            "managed" -> ManagedBlockchainConfigurationProvider()
            else -> ManualBlockchainConfigurationProvider(nodeConfigProvider)
        }
    }
}