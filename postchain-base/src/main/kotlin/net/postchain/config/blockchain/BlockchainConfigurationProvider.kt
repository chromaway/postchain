package net.postchain.config.blockchain

/**
 * Provides configuration of specific blockchain like block-strategy, configuration-factory,
 * signers and gtx specific args
 */
interface BlockchainConfigurationProvider {
    fun getConfiguration(chainId: Long): ByteArray?
}