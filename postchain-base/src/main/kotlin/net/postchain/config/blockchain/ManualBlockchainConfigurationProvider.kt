// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.blockchain

import mu.KLogging
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.DatabaseAccess
import net.postchain.core.EContext

class ManualBlockchainConfigurationProvider : BlockchainConfigurationProvider {

    companion object : KLogging()

    override fun needsConfigurationChange(eContext: EContext, chainId: Long): Boolean {
        val height = DatabaseAccess.of(eContext).getLastBlockHeight(eContext)
        val currentConfigHeight = BaseConfigurationDataStore.findConfigurationHeightForBlock(eContext, height)
        val nextConfigHeight = BaseConfigurationDataStore.findConfigurationHeightForBlock(eContext, height + 1)
        logger.debug("needsConfigurationChange() - height: $height, next conf at: $nextConfigHeight (currentConfigHeight: $currentConfigHeight)")
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