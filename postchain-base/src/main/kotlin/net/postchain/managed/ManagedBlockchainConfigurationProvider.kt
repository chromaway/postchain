package net.postchain.managed

import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.blockchain.ManualBlockchainConfigurationProvider
import net.postchain.core.EContext

class ManagedBlockchainConfigurationProvider : BlockchainConfigurationProvider {

    private lateinit var dataSource: ManagedNodeDataSource
    private val systemProvider = ManualBlockchainConfigurationProvider()

    fun setDataSource(dataSource: ManagedNodeDataSource) {
        this.dataSource = dataSource
    }

    override fun getConfiguration(eContext: EContext, chainId: Long): ByteArray? {
        return if (chainId == 0L) {
            systemProvider.getConfiguration(eContext, chainId)
        } else {
            if (::dataSource.isInitialized) {
                check(eContext.chainID == chainId) { "chainID mismatch" }
                getConfigurationFromDataSource(eContext)
            } else {
                throw IllegalStateException("Using managed blockchain configuration provider before it's properly initialized")
            }
        }
    }

    override fun needsConfigurationChange(eContext: EContext, chainId: Long): Boolean {
        fun checkNeedConfChangeViaDataSource(): Boolean {
            val dba = DatabaseAccess.of(eContext)
            val blockchainRID = dba.getBlockchainRID(eContext)
            val height = dba.getLastBlockHeight(eContext)
            val nextConfigHeight = dataSource.findNextConfigurationHeight(blockchainRID!!, height)
            return (nextConfigHeight != null) && (nextConfigHeight == height + 1)
        }

        return if (chainId == 0L) {
            systemProvider.needsConfigurationChange(eContext, chainId)
        } else {
            if (::dataSource.isInitialized) {
                checkNeedConfChangeViaDataSource()
            } else {
                throw IllegalStateException("Using managed blockchain configuration provider before it's properly initialized")
            }
        }
    }

    private fun getConfigurationFromDataSource(eContext: EContext): ByteArray? {
        val dba = DatabaseAccess.of(eContext)
        /* val newCtx = BaseEContext(eContext.conn,
                eContext.chainID, eContext.nodeID, dba)*/
        val blockchainRID = dba.getBlockchainRID(eContext)
        val height = dba.getLastBlockHeight(eContext) + 1
        return dataSource.getConfiguration(blockchainRID!!, height)
    }

}