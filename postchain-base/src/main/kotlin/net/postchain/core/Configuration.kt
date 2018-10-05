package net.postchain.core

import net.postchain.base.Storage

/**
 * BlockchainConfiguration is a stateless objects which describes
 * an individual blockchain instance within Postchain system
 */
interface BlockchainConfiguration {
    val chainID: Long
    val traits: Set<String>

    fun decodeBlockHeader(rawBlockHeader: ByteArray): BlockHeader
    fun decodeWitness(rawWitness: ByteArray): BlockWitness
    fun getTransactionFactory(): TransactionFactory
    fun makeBlockBuilder(ctx: EContext): BlockBuilder
    fun makeBlockQueries(storage: Storage): BlockQueries
    fun initializeDB(ctx: EContext)
    fun getBlockBuildingStrategy(blockQueries: BlockQueries, txQueue: TransactionQueue): BlockBuildingStrategy
}

interface ConfigurationDataStore {
    fun findConfiguration(context: EContext, height: Long): ByteArray?
    fun getConfigurationData(context: EContext, height: Long): ByteArray?
    fun addConfigurationData(context: EContext, height: Long, data: ByteArray): Long
}

interface BlockchainConfigurationFactory {
    fun makeBlockchainConfiguration(configurationData: Any): BlockchainConfiguration
}
