package net.postchain.config.blockchain

import net.postchain.StorageBuilder
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.base.withReadConnection
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.NODE_ID_TODO

class ManualBlockchainConfigurationProvider(
        private val nodeConfigProvider: NodeConfigurationProvider
) : BlockchainConfigurationProvider {

    override fun getConfiguration(chainId: Long): ByteArray? {
        val storage = StorageBuilder.buildStorage(nodeConfigProvider.getConfiguration(), NODE_ID_TODO)
        val dbAccess = SQLDatabaseAccess()

        val configuration = withReadConnection(storage, chainId) { eContext ->
            val lastHeight = dbAccess.getLastBlockHeight(eContext)
            val nextHeight = BaseConfigurationDataStore.findConfiguration(eContext, lastHeight + 1)
            nextHeight?.let {
                BaseConfigurationDataStore.getConfigurationData(eContext, it)!!
            }
        }

        storage.close()

        return configuration
    }
}