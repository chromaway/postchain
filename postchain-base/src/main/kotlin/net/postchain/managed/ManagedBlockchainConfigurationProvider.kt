// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.managed

import mu.KLogging
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.blockchain.ManualBlockchainConfigurationProvider
import net.postchain.core.EContext

class ManagedBlockchainConfigurationProvider : BlockchainConfigurationProvider {

    private lateinit var dataSource: ManagedNodeDataSource
    private val systemProvider = ManualBlockchainConfigurationProvider()

    companion object: KLogging()

    fun setDataSource(dataSource: ManagedNodeDataSource) {
        this.dataSource = dataSource
    }

    override fun getConfiguration(eContext: EContext, chainId: Long): ByteArray? {
        return if (chainId == 0L) {
            systemProvider.getConfiguration(eContext, chainId)
        } else {
            systemProvider.getConfiguration(eContext, chainId)
                    ?: if (::dataSource.isInitialized) {
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
            val blockchainRID = dba.getBlockchainRid(eContext)
            val height = dba.getLastBlockHeight(eContext)
            val nextConfigHeight = dataSource.findNextConfigurationHeight(blockchainRID!!.data, height)
            logger.debug("needsConfigurationChange() - height: $height, next conf at: $nextConfigHeight")
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
        val blockchainRID = dba.getBlockchainRid(eContext)
        val height = dba.getLastBlockHeight(eContext) + 1
        return dataSource.getConfiguration(blockchainRID!!.data, height)
    }

}