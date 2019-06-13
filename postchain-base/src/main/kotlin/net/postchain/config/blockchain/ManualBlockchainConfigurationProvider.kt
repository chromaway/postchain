package net.postchain.config.blockchain

import net.postchain.StorageBuilder
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.withReadConnection
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.EContext
import net.postchain.core.NODE_ID_TODO

class ManualBlockchainConfigurationProvider(
        private val nodeConfigProvider: NodeConfigurationProvider
) : BlockchainConfigurationProvider {

    override fun needsConfigurationChange(eContext: EContext, chainId: Long): Boolean {
        val lastHeight = DatabaseAccess.of(eContext).getLastBlockHeight(eContext)
        val curConfId = BaseConfigurationDataStore.findConfiguration(eContext, lastHeight)
        val nextConfId = BaseConfigurationDataStore.findConfiguration(eContext, lastHeight + 1)
        return  (curConfId != nextConfId)
    }

    override fun getConfiguration(eContext: EContext, chainId: Long): ByteArray? {
        val lastHeight = DatabaseAccess.of(eContext).getLastBlockHeight(eContext)
        val nextHeight = BaseConfigurationDataStore.findConfiguration(eContext, lastHeight + 1)
        return nextHeight?.let {
            BaseConfigurationDataStore.getConfigurationData(eContext, it)!!
        }
    }
}