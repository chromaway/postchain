package net.postchain.config.blockchain

import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.DatabaseAccess
import net.postchain.core.EContext

class ManualBlockchainConfigurationProvider : BlockchainConfigurationProvider {

    override fun needsConfigurationChange(eContext: EContext, chainId: Long): Boolean {
        val lastHeight = DatabaseAccess.of(eContext).getLastBlockHeight(eContext)
        val currentConfigHeight = BaseConfigurationDataStore.findConfiguration(eContext, lastHeight)
        val nextConfigHeight = BaseConfigurationDataStore.findConfiguration(eContext, lastHeight + 1)
        return (currentConfigHeight != nextConfigHeight)
    }

    override fun getConfiguration(eContext: EContext, chainId: Long): ByteArray? {
        val lastHeight = DatabaseAccess.of(eContext).getLastBlockHeight(eContext)
        val nextHeight = BaseConfigurationDataStore.findConfiguration(eContext, lastHeight + 1)
        return nextHeight?.let {
            BaseConfigurationDataStore.getConfigurationData(eContext, it)!!
        }
    }
}