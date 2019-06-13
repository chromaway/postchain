package net.postchain.config.blockchain

import net.postchain.core.EContext

class ManagedBlockchainConfigurationProvider : BlockchainConfigurationProvider {

    override fun getConfiguration(eContext: EContext, chainId: Long): ByteArray? {
        TODO("REMOVE THIS")
    }

    override fun needsConfigurationChange(eContext: EContext, chainId: Long): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

