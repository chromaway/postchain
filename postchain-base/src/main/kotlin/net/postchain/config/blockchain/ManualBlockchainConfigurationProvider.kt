package net.postchain.config.blockchain

import mu.KLogging
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.DatabaseAccess
import net.postchain.core.EContext

class ManualBlockchainConfigurationProvider : BlockchainConfigurationProvider {

    companion object : KLogging()

    override fun needsConfigurationChange(eContext: EContext, chainId: Long): Boolean {
        val lastHeight = DatabaseAccess.of(eContext).getLastBlockHeight(eContext)
        val currentConfigHeight = BaseConfigurationDataStore.findConfigurationHeightForBlock(eContext, lastHeight)
        val nextConfigHeight = BaseConfigurationDataStore.findConfigurationHeightForBlock(eContext, lastHeight + 1)

        logger.debug { "lastHeight: $lastHeight, currentConfigHeight: $currentConfigHeight, nextConfigHeight: $nextConfigHeight" }

        return (currentConfigHeight != nextConfigHeight)
    }

    override fun getConfiguration(eContext: EContext, chainId: Long): ByteArray? {
        val lastHeight = DatabaseAccess.of(eContext).getLastBlockHeight(eContext)
        val nextHeight = BaseConfigurationDataStore.findConfigurationHeightForBlock(eContext, lastHeight + 1)
        return nextHeight?.let {
            BaseConfigurationDataStore.getConfigurationData(eContext, it)!!
        }
    }
}