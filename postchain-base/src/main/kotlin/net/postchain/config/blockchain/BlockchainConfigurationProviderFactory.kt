package net.postchain.config.blockchain

import net.postchain.config.blockchain.BlockchainConfigProviders.Managed
import net.postchain.config.blockchain.BlockchainConfigProviders.Manual
import net.postchain.config.node.NodeConfigurationProvider

object BlockchainConfigurationProviderFactory {

    fun createProvider(nodeConfigProvider: NodeConfigurationProvider): BlockchainConfigurationProvider {
        val provider = nodeConfigProvider.getConfiguration().blockchainConfigProvider
        return when (provider.toLowerCase()) {
            Manual.name.toLowerCase() -> ManualBlockchainConfigurationProvider()
            Managed.name.toLowerCase() -> TODO("REMOVE THIS")
            else -> ManualBlockchainConfigurationProvider()
        }
    }
}