// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.blockchain

import net.postchain.core.EContext

/**
 * Provides configuration of specific blockchain like block-strategy, configuration-factory,
 * signers and gtx specific args
 */
interface BlockchainConfigurationProvider {
    fun getConfiguration(eContext: EContext, chainId: Long): ByteArray?
    fun needsConfigurationChange(eContext: EContext, chainId: Long): Boolean
}